package com.tencent.bk.devops.atom;

import com.tencent.bk.devops.atom.api.SdkEnv;
import com.tencent.bk.devops.atom.callback.CallBackRunner;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.exception.AtomException;
import com.tencent.bk.devops.atom.pojo.AtomBaseParam;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.CallBackWorker;
import com.tencent.bk.devops.atom.spi.ServiceLoader;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import kotlin.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * 插件执行入口类
 *
 * @version 1.0
 */
@SuppressWarnings("all")
public class AtomRunner {

    private static final Logger logger = LoggerFactory.getLogger(AtomRunner.class);

    public static void main(String[] args) throws IOException {
        TaskAtom atom = ServiceLoader.load(TaskAtom.class, false);
        AtomService annotation = atom.getClass().getAnnotation(AtomService.class);
        Class<? extends AtomBaseParam> tClass = annotation.paramClass();
        SdkEnv.init();
        AtomContext<? extends AtomBaseParam> context = getContext(tClass);
        try {
            atom.execute(context);
            executeCallBack(context);
        } catch (Throwable e) {
            logger.error("Unknown Error：{}", e.getMessage());
            e.printStackTrace();
            context.getResult().setStatus(Status.error);
            context.getResult().setMessage("Unknown Error：" + e.getMessage());
        } finally {
            context.persistent();
        }
    }

    private static void executeCallBack(AtomContext context) {
        try {
            final List<CallBackWorker> workers = getCallBackWorkers();
            if (workers.isEmpty())
                return;
            final ExecutorService pool = new ForkJoinPool(workers.size());
            logger.info("开始执行回调任务 {} 个", workers.size());
            final CompletableFuture<Pair<String, AtomResult>>[] futureList = workers.stream().map(worker ->
                CompletableFuture.supplyAsync(new CallBackRunner(worker, context), pool)
            ).toArray(CompletableFuture[]::new);
            // 确保所有任务结束
            CompletableFuture.allOf(futureList).join();
            final List<Pair<String, AtomResult>> results = Arrays.stream(futureList)
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            String callbackFailedMsg = results.stream().filter(stringAtomResultPair -> !Status.success.equals(stringAtomResultPair.getSecond().getStatus())).map(pair -> {
                    context.getResult().setStatus(Status.error);
                    final AtomResult atomResult = pair.getSecond();
                    final String name = pair.getFirst();
                    return new StringBuilder(name).append(": ").append(atomResult.getMessage());
                }
            ).collect(Collectors.joining("\n"));
            context.getResult().setMessage(callbackFailedMsg);
        } catch (Exception e) {
            Throwable exception = e;
            Throwable cause = exception.getCause();
            if (exception instanceof CompletionException && cause != null) {
                exception = cause;
            }
            logger.error("创建回调任务异常： {}", exception.getMessage());
            throw new AtomException("创建回调任务异常", exception);
        }
    }

    private static List<CallBackWorker> getCallBackWorkers() {
        return ServiceLoader.loadList(CallBackWorker.class);
    }

    private static <T extends AtomBaseParam> AtomContext<T> getContext(Class<T> tClass) throws IOException {
        return new AtomContext<>(tClass);
    }
}
