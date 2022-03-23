package com.tencent.bk.devops.atom.pojo;

import com.tencent.bk.devops.atom.pojo.AtomBaseParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 插件参数定义
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AtomParam extends AtomBaseParam{
    private Integer featureOption; // 群组结构选项 0 | 1 | 2
    private String sourceGroupUrl; // 源分组URL
    private String rootId;         // 根群组Id
    private String repoUrlList;        // 代码库地址列表
    private boolean ifSetAllConfig; //是否全量迁移配置文件
    private String configUrlList; //待迁移部分配置文件列表
    private String projectChName; // 项目中文名
    private String projectEnName; // 项目英文名
    private String backendUrlList; // 后端地址列表
    private String frontendUrlList; // 前端地址列表
    private String testUrlList; // 测试地址列表

}
