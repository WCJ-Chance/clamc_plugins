package com.tencent.bk.devops.atom.pojo.quality;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QualityValue {

    /**
     * 数值
     */
    private String value;

    public QualityValue(String value) {
        this.value = value;
    }
}
