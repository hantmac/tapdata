package io.tapdata.js.connector.server.function.support;

import io.tapdata.entity.error.CoreException;
import io.tapdata.entity.event.TapEvent;
import io.tapdata.entity.schema.TapTable;
import io.tapdata.entity.utils.DataMap;
import io.tapdata.js.connector.JSConnector;
import io.tapdata.js.connector.base.CustomEventMessage;
import io.tapdata.js.connector.base.ScriptCore;
import io.tapdata.js.connector.iengine.LoadJavaScripter;
import io.tapdata.js.connector.server.function.FunctionBase;
import io.tapdata.js.connector.server.function.FunctionSupport;
import io.tapdata.js.connector.server.function.JSFunctionNames;
import io.tapdata.js.connector.server.sender.BatchReadSender;
import io.tapdata.kit.EmptyKit;
import io.tapdata.pdk.apis.context.TapConnectorContext;
import io.tapdata.pdk.apis.functions.connector.source.BatchReadFunction;

import javax.script.ScriptEngine;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class JSBatchReadFunction extends FunctionBase implements FunctionSupport<BatchReadFunction> {
    AtomicBoolean isAlive = new AtomicBoolean(true);

    public JSBatchReadFunction isAlive(AtomicBoolean isAlive) {
        this.isAlive = isAlive;
        return this;
    }

    private JSBatchReadFunction() {
        super();
        super.functionName = JSFunctionNames.BatchReadFunction;
    }

    @Override
    public BatchReadFunction function(LoadJavaScripter javaScripter) {
        if (super.hasNotSupport(javaScripter)) return null;
        return this::batchRead;
    }

    private void batchRead(TapConnectorContext context, TapTable table, Object offset, int batchCount, BiConsumer<List<TapEvent>, Object> eventsOffsetConsumer) {
        if (Objects.isNull(context)) {
            throw new CoreException("TapConnectorContext cannot not be empty.");
        }
        if (Objects.isNull(table)) {
            throw new CoreException("TapTable cannot not be empty.");
        }
        ScriptEngine scriptEngine = javaScripter.scriptEngine();
        ScriptCore scriptCore = new ScriptCore();
        scriptEngine.put("core", scriptCore);
        AtomicReference<Throwable> scriptException = new AtomicReference<>();
        AtomicReference<Object> contextMap = new AtomicReference<>(offset);
        BatchReadSender sender = new BatchReadSender().core(scriptCore);
        Runnable runnable = () -> {
            try {
//                synchronized (JSConnector.execLock) {
                    super.javaScripter.invoker(
                            JSFunctionNames.BatchReadFunction.jsName(),
                            Optional.ofNullable(context.getConnectionConfig()).orElse(new DataMap()),
                            Optional.ofNullable(context.getNodeConfig()).orElse(new DataMap()),
                            Optional.ofNullable(contextMap.get()).orElse(new HashMap<>()),
                            table.getId(),
                            batchCount,
                            sender
                    );
//                }
                Thread.currentThread().stop();
            } catch (Exception e) {
                scriptException.set(e);
            }
        };
        Thread t = new Thread(runnable);
        t.start();
        List<TapEvent> eventList = new ArrayList<>();
        Object lastContextMap = null;
        while (isAlive.get() && t.isAlive()) {
            try {
                CustomEventMessage message = null;
                try {
                    message = scriptCore.getEventQueue().poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                if (EmptyKit.isNotNull(message)) {
                    eventList.add(message.getTapEvent());
                    lastContextMap = message.getContextMap();
                    if (eventList.size() == batchCount) {
                        eventsOffsetConsumer.accept(eventList, lastContextMap);
                        eventList = new ArrayList<>();
                        contextMap.set(lastContextMap);
                    }
                }
            } catch (Exception e) {
                break;
            }
        }
        if (EmptyKit.isNotNull(scriptException.get())) {
            throw new RuntimeException(scriptException.get());
        }
        if (isAlive.get() && EmptyKit.isNotEmpty(eventList)) {
            eventsOffsetConsumer.accept(eventList, lastContextMap);
            contextMap.set(lastContextMap);
        }
        if (t.isAlive()) {
            t.stop();
        }
    }

    public static BatchReadFunction create(LoadJavaScripter loadJavaScripter, AtomicBoolean isAlive) {
        return new JSBatchReadFunction().isAlive(isAlive).function(loadJavaScripter);
    }
}
