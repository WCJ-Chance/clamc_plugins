package com.tencent.bk.devops.atom.spi;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.callback.BaseCallBackParam;
import com.tencent.bk.devops.atom.pojo.AtomBaseParam;
import com.tencent.bk.devops.atom.pojo.AtomResult;

public interface CallBackWorker<T extends AtomBaseParam, K extends BaseCallBackParam> {

    /**
     * 执行回调逻辑
     *
     * @param atomContext   插件上下文
     * @param callBackParam 回调结果
     * @return
     */
    AtomResult execute(AtomContext<T> atomContext, K callBackParam);


}
