package com.tencent.bk.devops.atom.callback;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.exception.AtomException;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.spi.CallBackService;
import com.tencent.bk.devops.atom.spi.CallBackWorker;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import com.tencent.bk.devops.plugin.api.impl.CallBackApi;
import com.tencent.bk.devops.plugin.pojo.callback.CallbackData;
import com.tencent.bk.devops.plugin.pojo.callback.CallbackStatus;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


@SuppressWarnings("all")
public class CallBackRunner implements Supplier<Pair<String, AtomResult>> {
    /**
     * 默认超时时间 单位：秒
     * 默认超时30分钟
     */
    public static final long CALL_BACK_TIME_OUT = 1800L;
    private final static Logger logger = LoggerFactory.getLogger(CallBackRunner.class);
    // 回调api
    private final CallBackApi callBackApi = new CallBackApi();
    private final CallBackWorker worker;
    private final AtomContext atomContext;
    private final String taskId;

    public CallBackRunner(
        @NotNull CallBackWorker worker,
        @NotNull AtomContext atomContext) {
        this.worker = worker;
        this.atomContext = atomContext;
        this.taskId = atomContext.getParam().getPipelineTaskId();
    }

    @Override
    @NotNull
    public Pair<String, AtomResult> get() {
        CallBackService annotation = worker.getClass()
            .getAnnotation(CallBackService.class);
        // 根据注解找到name
        String name = annotation.name();
        long timeout = annotation.timeout();
        String uniqueKey = atomContext.getCallBackUniqueKey(name);
        try {
            if (uniqueKey == null) {
                throw new AtomException("未检测到回调服务唯一标识，回调服务无法启动");
            }
            // 在微服务中创建回调任务
            if (!callBackApi.createCallback(name, uniqueKey, taskId)) {
                throw new AtomException("创建回调任务失败，回调服务无法启动");
            }

            Class<? extends BaseCallBackParam> tClass = annotation.paramClass();
            BaseCallBackParam callBackParam = JsonUtil
                .fromJson(getCallBackResult(uniqueKey, timeout), tClass);
            AtomResult result = worker.execute(atomContext, callBackParam);
            return new Pair<>(name, result);
        } catch (Exception e) {
            AtomResult result = new AtomResult();
            result.setStatus(Status.failure);
            result.setMessage(name + " 回调服务执行失败: " + e.getMessage());
            return new Pair<>(name, result);
        }
    }

    /**
     * @param uniqueKey
     * @param timeout   单位秒
     * @return
     * @throws InterruptedException
     */
    private String getCallBackResult(final String uniqueKey, long timeout) throws InterruptedException {
        if (timeout < 0) timeout = 10;
        long timeWait = 10L;
        long count = timeout / timeWait;
        CallbackData callbackData = null;
        for (int i = 0; i < count; i++) {
            callbackData = callBackApi
                .claimCallbackRequestBody(uniqueKey, taskId);
            if (callbackData.getStatus() == CallbackStatus.SUCCESS) {
                break;
            }
            TimeUnit.SECONDS.sleep(10);
        }
        //      成功就返回，失败抛异常
        if (callbackData.getStatus() != CallbackStatus.SUCCESS) {
            throw new AtomException("等待回调超时");
        }
        return callbackData.getData();
    }
}
