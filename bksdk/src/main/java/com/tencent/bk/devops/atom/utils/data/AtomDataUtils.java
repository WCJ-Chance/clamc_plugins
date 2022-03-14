package com.tencent.bk.devops.atom.utils.data;

import com.tencent.bk.devops.atom.api.SdkEnv;
import com.tencent.bk.devops.atom.utils.http.OkHttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 插件业务数据操作工具类
 */
public class AtomDataUtils {

    private final static Logger logger = LoggerFactory.getLogger(AtomDataUtils.class);

    /**
     * 保存流水线插件业务数据
     *
     * @param pipelineId      流水线ID
     * @param pipelineBuildId 流水线构建ID
     * @param pipelineTaskId  流水线插件ID
     * @param atomCode        流水线插件CODE
     * @param jsonParam       要保存的业务数据，json格式
     * @return
     */
    public static String saveData(String pipelineId, String pipelineBuildId, String pipelineTaskId, String atomCode, String jsonParam) {
        String saveDataUrl = String.format("%s/ms/store/api/service/pipeline/atom/data/%s/%s/%s/%s",
            SdkEnv.getGatewayHost(),
            pipelineId,
            pipelineBuildId,
            pipelineTaskId,
            atomCode);
        return OkHttpUtils.doPost(saveDataUrl, jsonParam);
    }

    /**
     * 获取流水线插件业务数据
     *
     * @param jsonParam 流水线插件信息，json格式
     * @return
     */
    public static String getData(String jsonParam) {
        String getDataUrl = String.format("%s/ms/store/api/service/pipeline/atom/data", SdkEnv.getGatewayHost());
        return OkHttpUtils.doPost(getDataUrl, jsonParam);
    }

}
