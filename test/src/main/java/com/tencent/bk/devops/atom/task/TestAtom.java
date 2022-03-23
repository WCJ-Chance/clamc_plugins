package com.tencent.bk.devops.atom.task;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.api.impl.CredentialApi;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.pojo.Result;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import com.tencent.bk.devops.atom.task.pojo.AtomParam;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @version 1.0
 */
@AtomService(paramClass = AtomParam.class)
public class TestAtom implements TaskAtom<AtomParam> {

    private final static Logger logger = LoggerFactory.getLogger(TestAtom.class);

    /**
     * 执行主入口
     *
     * @param atomContext 插件上下文
     */
    @SneakyThrows
    @Override
    public void execute(AtomContext<AtomParam> atomContext) throws IOException {
        // 1.1 拿到请求参数
        AtomParam param = atomContext.getParam();
        logger.info("the param is :{}", JsonUtil.toJson(param));
        // 1.2 拿到初始化好的返回结果对象
        AtomResult result = atomContext.getResult();
        // 2. 校验参数失败直接返回
        checkParam(param, result);
        if (result.getStatus() != Status.success) {
            return;
        }
        CredentialApi credentialApi = new CredentialApi();
        Result<Map<String, String>> result1 = credentialApi.getCredential(param.getTicketId());
        while (result1.isOk()) {
            logger.info(result1.getData().get("access_token"));
            result1 = credentialApi.getCredential(param.getTicketId());
        }
    }

    /**
     * 检查参数
     *
     * @param param  请求参数
     * @param result 结果
     */
    private void checkParam(AtomParam param, AtomResult result) {
        // 参数检查
//        if (StringUtils.isBlank(param.getBasicPath())) {
//            result.setStatus(Status.failure);// 状态设置为失败
//            result.setMessage("搜索目录不能为空!"); // 失败信息回传给插件执行框架会打印出结果
//        }
    }

}
