package com.tencent.bk.devops.atom.spi;


import com.tencent.bk.devops.atom.callback.BaseCallBackParam;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.tencent.bk.devops.atom.callback.CallBackRunner.CALL_BACK_TIME_OUT;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CallBackService {

    String name();

    /**
     * 排序顺序
     *
     * @return sortNo
     */
    int order() default 0;

    /**
     * 等待回调的超时时间 单位：秒
     * 默认30分钟
     *
     * @return 超时时间
     */
    long timeout() default CALL_BACK_TIME_OUT;

    /**
     * 参数类
     */
    Class<? extends BaseCallBackParam> paramClass();
}
