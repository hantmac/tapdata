## **Connection configuration help**
### **1. Supported version**
Databend v0.9 or later

### **2. Connection configuration
You must have there params to connect databend.

| param           | explain                                                       | required |
|-----------------|---------------------------------------------------------------|----------|
| host            | databend host                                                 | Yes      |
| user            | the user                                                      | Yes      |
| password        | the password                                                  | Yes      |
| port            | the port for databend host                                    | Not      |
| database        | which database to sync                                        | Yes      |
| addtionalString | some other params in DSN such as ?xxx=yyy&wait_time_secs=10s& | Not      |

If you use databend cloud, you can find these params from [this doc](https://docs.databend.com/using-databend-cloud/warehouses/connecting-a-warehouse).