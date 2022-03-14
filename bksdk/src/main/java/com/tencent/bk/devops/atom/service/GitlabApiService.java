package com.tencent.bk.devops.atom.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

@Data
public class GitlabApiService {
    private final static Logger logger = LoggerFactory.getLogger(GitlabApiService.class);

    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");

    private String parentId;
    private String parentPath;
    private String backendId;
    private String uiId;
    private String testId;
    private String targetGitLabUrl;
    private String targetGitLabAccessToken;

    protected final ObjectMapper objectMapper = new ObjectMapper();


    @SneakyThrows
    public GitlabApiService(String targetGitLabUrl, String targetGitLabAccessToken, String parentName, String parentPath) {
        this.targetGitLabUrl = targetGitLabUrl;
        this.targetGitLabAccessToken = targetGitLabAccessToken;
        this.parentPath = parentPath;
        String pId = searchGroupByPath(parentPath);
        if (pId == null) { // 父群组不存在
            logger.info("现在开始创建项目群组···");
            parentId = createGroup(parentName, parentPath);
            backendId = createSubGroup("Backend");
            uiId = createSubGroup("UI");
            testId = createSubGroup("Test");
        }
        else {
            logger.info("检测到项目群组已存在");
            parentId = pId;
            JsonNode subNode = listSubGroups(parentId);
            for (JsonNode jn : subNode
                 ) {
                String path = jn.get("path").asText();
                String id = jn.get("id").asText();
                switch (path) {
                    case "Backend":
                        backendId = id;
                        break;
                    case "UI":
                        uiId = id;
                        break;
                    case "Test":
                        testId = id;
                        break;
                }
            }
        }
    }
    @SneakyThrows
    public GitlabApiService(String targetGitLabUrl, String targetGitLabAccessToken, String parentName, String parentPath, String rootId) {
        this.targetGitLabUrl = targetGitLabUrl;
        this.targetGitLabAccessToken = targetGitLabAccessToken;
        this.parentPath = parentPath;
        String pId = searchGroupByPath(parentPath);
        if (pId == null) { // 父群组不存在
            logger.info("现在开始创建项目群组···");
            parentId = createGroup(parentName, parentPath, rootId);
            backendId = createSubGroup("Backend");
            uiId = createSubGroup("UI");
            testId = createSubGroup("Test");
        }
        else {
            logger.info("检测到项目群组已存在");
            parentId = pId;
            JsonNode subNode = listSubGroups(parentId);
            for (JsonNode jn : subNode
                 ) {
                String path = jn.get("path").asText();
                String id = jn.get("id").asText();
                switch (path) {
                    case "Backend":
                        backendId = id;
                        break;
                    case "UI":
                        uiId = id;
                        break;
                    case "Test":
                        testId = id;
                        break;
                }
            }
        }
    }


    @SneakyThrows
    public String searchGroupByPath(String path) {
        Request request = new Request.Builder()
                .url(targetGitLabUrl + "/api/v4/groups/?search=" + path)
                .get()
                .addHeader("PRIVATE-TOKEN", targetGitLabAccessToken)
                .build();
        Response res = client.newCall(request).execute();
        logger.info("searchGroupByPath --- GET {}/api/v4/groups/?search={}\n{}", targetGitLabUrl, path, res);
        if (res.isSuccessful()) {
            JsonNode resBody = parseJson(res);
            for (JsonNode jn : resBody
            ) {
                if (jn.get("path").asText().equals(path)) { // 确保查询到的群组path与入参完全一致！
                    logger.info("{}群组已存在\n:{}",path, jn.asText());
                    return jn.get("id").asText();
                }
            }
            logger.error("未找到{}群组", path);
        } else {
            logger.error("查询群组失败");
        }
        return null;
    }

    @SneakyThrows
    public String createGroup(String name, String path) {
        RequestBody body = RequestBody.create("name=" + name + "&path=" + path, mediaType);
        Request request = new Request.Builder()
                .url(targetGitLabUrl + "/api/v4/groups")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("PRIVATE-TOKEN", targetGitLabAccessToken)
                .build();
        Response res = client.newCall(request).execute();
        logger.info("createGroup --- POST {}/api/v4/groups name={}&path={} \n {}", targetGitLabUrl, name, path, res);
        return parseJson(res).get("id").asText();
    }

    @SneakyThrows
    private String createGroup(String name, String path, String rootId) {
        RequestBody body = RequestBody.create("name=" + name + "&path=" + path + "&parent_id=" + rootId, mediaType);
        Request request = new Request.Builder()
                .url(targetGitLabUrl + "/api/v4/groups")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("PRIVATE-TOKEN", targetGitLabAccessToken)
                .build();
        Response res = client.newCall(request).execute();
        logger.info("createGroup --- POST {}/api/v4/groups name={}&path={}&parent_id={} \n {}", targetGitLabUrl, name, path, rootId, res);
        return parseJson(res).get("id").asText();
    }

    @SneakyThrows
    public String createSubGroup(String path) {
        RequestBody body = RequestBody.create("name=" + path + "&path=" + path + "&parent_id=" + parentId, mediaType);
        Request request = new Request.Builder()
                .url(targetGitLabUrl + "/api/v4/groups")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("PRIVATE-TOKEN", targetGitLabAccessToken)
                .build();
        Response res = client.newCall(request).execute();
        logger.info("createSubGroup --- POST {}/api/v4/groups name={}&path={}&parent_id={} \n {}", targetGitLabUrl, path, path, parentId, res);
        return parseJson(res).get("id").asText();
    }

    @SneakyThrows
    public JsonNode listSubGroups(String parentId) {
        Request request = new Request.Builder()
                .url(targetGitLabUrl + "/api/v4/groups/" + parentId + "/subgroups")
                .get()
                .addHeader("PRIVATE-TOKEN", targetGitLabAccessToken)
                .build();
        Response res = client.newCall(request).execute();
        return parseJson(res);
    }


    @SneakyThrows
    public String projectIsExsit(String path) {
        Request request = new Request.Builder()
                .url(targetGitLabUrl + "/api/v4/projects?search=" + path)
                .get()
                .addHeader("PRIVATE-TOKEN", targetGitLabAccessToken)
                .build();
        Response res = client.newCall(request).execute();
        JsonNode resBody = parseJson(res);
        for (JsonNode jn : resBody
             ) {
            String pathWithNamespace = jn.get("path_with_namespace").asText();
            String[] pathSlice = pathWithNamespace.split("/");
            for (String p : pathSlice
                 ) {
                if (p.equals(parentPath)) {
                    return jn.get("http_url_to_repo").asText();
                }
            }
        }
        return null;
    }

    @SneakyThrows
    public String createProject(String name, String namespaceId) {
        RequestBody body = RequestBody.create("name=" + name + "&namespace_id=" + namespaceId + "&undefined="
                , mediaType);
        Request request = new Request.Builder()
                .url(targetGitLabUrl + "/api/v4/projects")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("PRIVATE-TOKEN", targetGitLabAccessToken)
                .build();
        Response res = client.newCall(request).execute();
        JsonNode resBody = parseJson(res);
        if (res.isSuccessful() && !resBody.isEmpty()) {
            return resBody.get("http_url_to_repo").asText();
        }
        logger.error("创建代码库{}失败", name);
        return null;
    }

    /**
     * 通过Response获取JsonNode
     * @param response response实体
     * @return response解析出的JsonNode
     */
    public JsonNode parseJson(Response response) {
        if (response != null) {
            ResponseBody body = response.body();
            String bodyString = null;
            if (body != null) {
                try {
                    bodyString = body.string();
                    if (response.isSuccessful()) {
                        return objectMapper.readTree(bodyString);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logger.error("response is {}", response);
                logger.error(bodyString);
            }
        }
        return null;
    }
}
