package com.tencent.bk.devops.atom.task.pojo;

import com.tencent.bk.devops.atom.pojo.AtomBaseParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 插件参数定义
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AtomParam extends AtomBaseParam {
    private String basicPath; // 搜索目录
}

