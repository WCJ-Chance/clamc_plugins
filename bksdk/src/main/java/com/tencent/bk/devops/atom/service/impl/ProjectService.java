package com.tencent.bk.devops.atom.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.tencent.bk.devops.atom.service.BaseRequestService;
import lombok.Data;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProjectService extends BaseRequestService {
    private final static Logger logger = LoggerFactory.getLogger(ProjectService.class);

    private String projectName; // 项目名称
    private String platform;    // 业务平台名称
    private String projectCode; // 项目标识


    public ProjectService(String projectId, String bktoken) {
        // 指定登陆地址
        super.baseUrl = "http://devops.clamc.com";
        setCookie(bktoken);
        if (StringUtils.isEmpty(cookie)) {
            logger.error("用户未登录，请先登录在尝试！ProjectService初始化失败！");
        } else {
            cookie += ";X-DEVOPS-TENANT-ID=bk_ci;";
            String url = super.baseUrl + "/ms/project/api/user/projectProps/" + projectId;
            Response response = super.doGetRaw(url, null);
            JsonNode jsonNode = super.parseJson(response);
            if (jsonNode.get("status").asInt() == 0) {
                JsonNode props = jsonNode.get("data").get("props");
                for (JsonNode prop : props) {
                    String propId = prop.get("propId").asText();
                    String option = prop.get("values").get("option").asText();
                    if (propId.equals("142afd8672234c6a884c5bebc7c7b5e5")) {
                        this.platform = option;
                    } else if (propId.equals("projectName")) {
                        this.projectName = option;
                    } else if (propId.equals("englishNameCustom")) {
                        this.projectCode = option;
                    }
                }
            } else {
                logger.error("未获取到对应项目信息，ProjectService初始化失败!");
            }
        }
    }

    public Map<String, String> getUserRoleInProject(String projectId) {

        Map<String, String> characterC2EMap = new HashMap<>();
        characterC2EMap.put("技术负责人", "TechLeader");
        characterC2EMap.put("技术助理", "TechAssist");
        characterC2EMap.put("开发人员", "Developer");
        characterC2EMap.put("测试人员", "Tester");
        characterC2EMap.put("需求分析人员", "Analysis");


        String url = super.baseUrl + "/ms/permission/api/user/group/" + projectId + "/users";
        cookie += ";X-DEVOPS-TENANT-ID=bk_ci;";
        Response response = doGetRaw(url, null);
        JsonNode jsonNode = parseJson(response);
        if (jsonNode == null) {
            logger.error("获取项目内成员信息失败");
            return null;
        }
        try {
            if (jsonNode.get("status").asInt() == 0) {
                Map<String, String> userRoleMap = new HashMap<>();
                for (JsonNode user : jsonNode.get("data")) {
                    String userId = user.get("userId").asText();
                    boolean admin = user.get("admin").asBoolean();
                    if (admin) {
                        String projectManager = userRoleMap.getOrDefault("ProjectManager", null);
                        if (projectManager == null) {
                            userRoleMap.put("ProjectManager", userId);
                        } else {
                            userRoleMap.put("ProjectManager", projectManager + "," + userId);
                        }
                    }
                    JsonNode groups = user.get("groups");
                    for (JsonNode group : groups) {
                        if (group.get("belongInstance").asText().equals(projectId)) { // TODO:这个条件判断是否多余？
                            String characterCnName = group.get("name").asText();
                            String characterEnName = characterC2EMap.get(characterCnName);
                            String roleUser = userRoleMap.getOrDefault(characterEnName, null);
                            if (roleUser == null) {
                                userRoleMap.put(characterEnName, userId);
                            } else {
                                userRoleMap.put(characterEnName, roleUser + "," + userId);
                            }
                        }
                    }
                }
                return userRoleMap;
            } else {
                logger.error("获取项目成员信息失败");
                logger.error(jsonNode.toPrettyString());
                return null;
            }
        }catch (Exception exception) {
            logger.error("解析项目成员信息失败！");
            exception.printStackTrace();
            return null;
        }
    }
}
