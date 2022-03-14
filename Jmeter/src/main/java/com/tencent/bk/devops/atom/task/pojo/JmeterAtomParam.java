package com.tencent.bk.devops.atom.task.pojo;

import com.tencent.bk.devops.atom.pojo.AtomBaseParam;
import com.tencent.bk.devops.atom.task.JmeterAtom;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 插件参数定义
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JmeterAtomParam extends AtomBaseParam {
    /**
     * 以下请求参数只是示例，具体可以删除修改成你要的参数
     */
    private String path; //jmx文件路径

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
