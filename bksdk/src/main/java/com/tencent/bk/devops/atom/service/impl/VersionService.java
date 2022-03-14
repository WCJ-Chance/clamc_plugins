package com.tencent.bk.devops.atom.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.tencent.bk.devops.atom.service.BaseRequestService;
import lombok.Data;
import lombok.SneakyThrows;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class VersionService extends BaseRequestService {

    private final static Logger logger = LoggerFactory.getLogger(VersionService.class);

    private static String SEARCH_SPECIFIED_VERSION = "/ms/vteam/api/user/issue_version/%s/flat?content=%s";
    private static String SEARCH_SPECIFIED_ISSUES = "/ms/vteam/api/user/issue/%s/version_iteration/VERSION/%s?";
    private static String SEARCH_NEXT_NODE = "/ms/vteam/api/user/issue_direction/%s/%s/state";
    private static String NEXT = "/ms/vteam/api/user/issue_direction/%s/next";

    private String versionName;
    private JsonNode versionNode;
    private String versionId;
    private int total;


    public VersionService(String projectName, String bktoken, String versionName) {
        super.cookie = bktoken;
        super.projectName = projectName;
        this.versionName = versionName;
        this.versionNode = getVersionNode(); // version实体（记录versionId等信息）
        this.versionId = getVersionId();
        this.total = getVersionTotal();
    }


    /**
     * 获取指定version的Json实体信息，记录版本Id及所含工作项等重要信息
     * @return
     */
    @SneakyThrows
    public JsonNode getVersionNode() {
        if (StringUtils.isEmpty(cookie)) {
            logger.error("用户未登录，请先登录后再尝试");
            return null;
        }
        String api = String.format(baseUrl + SEARCH_SPECIFIED_VERSION, projectName, versionName);
        Response res = super.doGetRaw(api, null);
        return super.parseJson(res);
    }

    /**
     * 通过versionNode获取versionId
     * @return
     */
    public String getVersionId() {
        if (versionNode.get("status").asText() != "0" || versionNode.get("data").get("content").isEmpty()) {
            logger.error("没有查到对应版本！");
            return null;
        }
        return versionNode.get("data").get("content").get(0).get("id").asText();
    }

    /**
     * 获取版本下的所有工作项Json实体
     * @return
     */
    public JsonNode getVersionIssuesNode() {
        String api = String.format(baseUrl + SEARCH_SPECIFIED_ISSUES, projectName, versionId);
        Response res = super.doPostRaw(api, "[]", null);
        return super.parseJson(res);
    }

    /**
     * 获取工作项状态流转列表
     * @param issueId
     * @return
     */
    public JsonNode getNextNode(String issueId) {
        String api = String.format(baseUrl + SEARCH_NEXT_NODE, projectName, issueId);
        Response res = super.doGetRaw(api, null);
        return super.parseJson(res);
    }

    /**
     * 执行单个工作项流转
     * @param issueId
     * @param nextNodeId
     * @return
     */
    public Response next(String issueId, String nextNodeId) {

        String api = String.format(baseUrl + NEXT, projectName);
        String requestBody = String.format("{\"issueId\":\"%s\",\"nextNodeId\":\"%s\",\"comment\":{\"atUser\":[],\"comment\":\"\"},\"directionFields\":[],\"operators\":[]}",
                issueId, nextNodeId);
        return super.doPostRaw(api, requestBody, null);
    }

    /**
     * 通过versionNode获取versionTotal
     * @return
     */
    public int getVersionTotal() {
        if (versionNode.get("status").asText() != "0" || versionNode.get("data").get("content").isEmpty()) {
            logger.error("没有查到对应版本！");
            return 0;
        }
        return versionNode.get("data").get("content").get(0).get("total").asInt();
    }
}
