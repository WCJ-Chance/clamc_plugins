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
    private Integer featureOption; // 群组结构选项 0 | 1
    private String rootId;         // 根群组Id
    private String projectCnName;  // 项目中文名
    private String projectEnName;  // 项目英文名
    private String backend;        // 后端代码库地址列表
    private String ui;             // 前端代码库地址列表
    private String test;           // 测试代码库地址列表
}
