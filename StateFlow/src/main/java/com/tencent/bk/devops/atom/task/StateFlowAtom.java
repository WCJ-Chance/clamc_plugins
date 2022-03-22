package com.tencent.bk.devops.atom.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.pojo.DataField;
import com.tencent.bk.devops.atom.pojo.ReportData;
import com.tencent.bk.devops.atom.pojo.quality.QualityValue;
import com.tencent.bk.devops.atom.service.impl.BkLoginService;
import com.tencent.bk.devops.atom.service.impl.VersionService;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import com.tencent.bk.devops.atom.task.pojo.AtomParam;
import com.tencent.bk.devops.atom.task.utils.CommandLineUtils;
import com.tencent.bk.devops.atom.task.utils.JFrogUtil;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Jmeter
 */
@AtomService(paramClass = AtomParam.class)
public class StateFlowAtom implements TaskAtom<AtomParam> {

    private final static Logger logger = LoggerFactory.getLogger(StateFlowAtom.class);

    /**
     * 执行主入口
     *
     * @param atomContext 插件上下文
     */
    @Override
    public void execute(AtomContext<AtomParam> atomContext) {
        // 1.1 拿到请求参数
        AtomParam param = atomContext.getParam();
        logger.info("the param is :{}", JsonUtil.toJson(param));
        // 1.2 拿到初始化好的返回结果对象
        AtomResult result = atomContext.getResult();
        // 2. 校验参数失败直接返回
//        checkParam(param, result);
//        if (result.getStatus() != Status.success) {
//            return;
//        }

        // 蓝鲸模拟登录
//        BkLoginService bkLoginService = new BkLoginService("http://devops-paas.clamc.com");
        BkLoginService bkLoginService = new BkLoginService("http://paas.bktest.com");
        // 获取csrfToken
//        String bkToken = bkLoginService.login("devops_api", "1qaz@WSX");
        String bkToken = bkLoginService.login("admin", "1qaz@WSX");
        VersionService versionService = new VersionService(param.getProjectName(), bkToken, param.getVersion());
        JsonNode issuesNode = versionService.getVersionIssuesNode();
        logger.info(issuesNode.asText());
        int success = 0;
        for (JsonNode jn : issuesNode.get("data").get("records")
        ) {
            String issueId = jn.get("property").get("id").get("value").asText();
            JsonNode nextNodeInfo = versionService.getNextNode(issueId);
            int flag = 0;
            String nextNodeId = "";
            for (JsonNode node : nextNodeInfo.get("data").get("data")
            ) {
                if (node.get("name").asText().equals(param.getTargetState())) {
                    flag = 1;
                    nextNodeId = node.get("id").asText();
                    break;
                }
            }
            if (flag == 0) {
                logger.error("没有在工作项{}流转节点列表中找到目标节点，请确认该工作项状态", jn.get("property").get("number").get("value").asText());
            } else {
                Response res = versionService.next(issueId, nextNodeId);
                if (res.isSuccessful()) {
                    logger.info("工作项{}状态成功流转到\"{}\"", jn.get("property").get("number").get("value").asText(), param.getTargetState());
                    success ++;
                } else {
                    logger.error("工作项{}状态流转到\"{}\"失败，请记得手动流转。", jn.get("property").get("number").get("value").asText(), param.getTargetState());
                }
            }
        }
        logger.info("当前版本下共有{}个工作项，本次自动流转成功的工作项有{}个，部分工作项可能提前进入终止节点，也可能是流转失败，请人工核对！！！",
                versionService.getVersionTotal(), success);
//    /**
//     * 检查参数
//     * @param param  请求参数
//     * @param result 结果
//     */
//    private void checkParam(AtomParam param, AtomResult result) {
////         参数检查
//        if (StringUtils.isBlank(param.getTargetState())||StringUtils.isBlank(param.getVersion())) {
//            result.setStatus(Status.failure);// 状态设置为失败
//            result.setMessage("目标状态或产品版本不能为空!"); // 失败信息回传给插件执行框架会打印出结果
//        }

        /*
         其他比如判空等要自己业务检测处理，否则后面执行可能会抛出异常，状态将会是 Status.error
         这种属于插件处理不到位，算是bug行为，需要插件的开发去定位
          */
    }
}
