package com.tapdata.tm.discovery.bean;

import lombok.Data;

@Data
public class DiscoveryQueryParam {
    /** 对象分类 */
    private String category;
    /** 对象类型 */
    private String type;
    /** 来源分类 */
    private String sourceCategory;
    /** 来源类型 */
    private String sourceType;
    /**  */
    private Integer page = 0;
    /**  */
    private Integer pageSize = 20;
    /** 模糊查询key */
    private String queryKey;
}
