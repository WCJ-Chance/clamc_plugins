package com.tencent.bk.devops.atom.task;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.pojo.DataField;
import com.tencent.bk.devops.atom.pojo.ReportData;
import com.tencent.bk.devops.atom.pojo.quality.QualityValue;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import com.tencent.bk.devops.atom.task.pojo.AtomParam;
import com.tencent.bk.devops.atom.task.utils.CommandLineUtils;
import com.tencent.bk.devops.atom.utils.http.OkHttpUtils;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * SonarQube代码检查
 */
@AtomService(paramClass = AtomParam.class)
public class SonarQubeQualityAtom implements TaskAtom<AtomParam> {

    private final static Logger logger = LoggerFactory.getLogger(SonarQubeQualityAtom.class);


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
        StringBuffer pipelineNameUN = new StringBuffer();
        //流水线名unicode编码
        String projectName = param.getProjectName() + "-" + param.getServiceName();
        logger.info("sonarProjectName:{}", projectName);
        for (int i = 0; i < projectName.length(); i++) {
            // 取出每一个字符
            char c = projectName.charAt(i);
            // 转换为unicode
            if (Integer.toHexString(c).length() == 4) {
                pipelineNameUN.append("\\u" + Integer.toHexString(c));
            } else if (Integer.toHexString(c).length() == 2) {
                pipelineNameUN.append("\\u00" + Integer.toHexString(c));
            }
        }
        String token = atomContext.getSensitiveConfParam("token");
        String sonarProjectPath = param.getBkWorkspace() + "/sonar-project.properties";
        if (!new File(sonarProjectPath).exists()) {
            try {
                new File(sonarProjectPath).createNewFile();
                File file = new File(sonarProjectPath);
                FileWriter fileWriter = new FileWriter(file);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String projectVersion = sdf.format(System.currentTimeMillis());
                String fileContent =
                        "sonar.projectKey=" + projectName + "\n" +
                                "sonar.projectName=" + pipelineNameUN + "\n" +
                                "sonar.projectVersion=" + projectVersion + "\n" +
                                "sonar.sources=.\n" +
                                "sonar.java.binaries=.\n" +
                                "sonar.sourceEncoding=UTF-8\n" +
                                "sonar.login=" + token;
                fileWriter.write(fileContent);
                fileWriter.flush();
                fileWriter.close();
            } catch (Exception e) {
                result.setStatus(Status.failure);
                result.setMessage(e.getMessage());
                return;
            }
        }

        //sonar-scanner代码检查
        logger.info("start sonar-scanner");
        logger.info("commandPath:" + param.getCommandPath());

        String sonarScannerLog = CommandLineUtils.execute(param.getCommandPath(), new File(param.getBkWorkspace()), false, "");
        logger.info("sonarScannerLog:\n" + sonarScannerLog);
        logger.info("end sonar-scanner");

        try {
            Thread.sleep(1 * 60 * 1000);//睡眠一分钟，保证最新扫描结果上传成功，后面调接口获取的是最新数据
        } catch (InterruptedException e) {
            result.setStatus(Status.failure);
            result.setMessage(e.getMessage());
            return;
        }

        // 接口请求认证
        final Base64.Encoder encoder = Base64.getEncoder();
        String encodedText = "";
        String loginString = token + ":";
        try {
            encodedText = encoder.encodeToString(loginString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map<String,String> map=new HashMap<String,String>();
        map.put("Authorization", "Basic " + encodedText);
        //获取扫描历史记录生成产出物报告
        getHistoryRecord(param, map, result, projectName);
        logger.info("sonarqube end....");
    }

    private String getProjectStatus(AtomParam param,  Map<String,String> map, AtomResult result, String projectKey) {
        String statusUrl = param.getSonarQubeIpPort() + "/api/qualitygates/project_status?projectKey=" + projectKey;
        String resultJson = OkHttpUtils.doGet(statusUrl, map);
        logger.info(resultJson);
        String status = JSONObject.parseObject(resultJson).getJSONObject("projectStatus").getString("status");
        logger.info("sonarqube scanner project status is " + status);
        if ("ERROR".equals(status)) {
            logger.error("未通过SonarQube质量阈！！！");
            result.setStatus(Status.failure);// 状态设置为失败
            result.setMessage("sonarqube scanner project failure");
        }
        return status;
    }

    /**
     * API获取历史记录，生成扫描结果，对接质量红线指标
     * @param param
     * @param result
     */
    private void getHistoryRecord(AtomParam param, Map<String,String> map, AtomResult result, String projectKey) {
        //API获取历史记录
        String componentUrl = param.getSonarQubeIpPort() + "/api/measures/component?additionalFields=metrics%2Cperiods&component=" + projectKey + "&metricKeys=bugs%2Cnew_bugs%2Cvulnerabilities%2Cnew_vulnerabilities%2Ccode_smells%2Cnew_code_smells%2Ccoverage%2Cnew_coverage%2Cduplicated_lines_density%2Cnew_duplicated_lines_density";
        String resultJson = OkHttpUtils.doGet(componentUrl, map);
        logger.info(resultJson);
        JSONArray measures = JSONObject.parseObject(resultJson).getJSONObject("component").getJSONArray("measures");
        //获取sonarqube扫描状态
        String status = getProjectStatus(param, map, result, projectKey);
        //产出物报告内容
        StringBuilder reportBody = new StringBuilder();
        String fileOutAddress = param.getSonarQubeIpPort() + "/dashboard?id=" + projectKey;
        reportBody.append("<h2>" + projectKey + "代码扫描结果:" + status +"</h2>");
        reportBody.append("<table class='tablex' border='1'><tr><th>指标</th><th>新增数</th><th>总数</th></tr>");
        //对接质量红线指标
        result.setType("quality");
        Map<String, QualityValue> quality = new HashMap<>();
        Map<String, String> resultMap = new HashMap<>();
        for (int i = 0; i < measures.size(); i++) {
            JSONObject measure = measures.getJSONObject(i);
            String metric = measure.getString("metric");
            if (metric.startsWith("new_")) {
                JSONArray periods = measure.getJSONArray("periods");
                for (int j = 0; j < periods.size(); j++) {
                    JSONObject period = periods.getJSONObject(j);
                    String value = period.getString("value");
                    logger.info("metric:" + metric + ", value:" + value);
                    quality.put(metric, new QualityValue(value));
                    resultMap.put(metric, value);
                }
            } else {
                String value = measure.getString("value");
                logger.info("metric:" + metric + ", value:" + value);
                quality.put(metric, new QualityValue(value));
                resultMap.put(metric, value);
            }
        }
        reportBody.append("<tr><td>").append("Bugs").append("</td><td>").append(resultMap.getOrDefault("new_bugs", "0")).append("</td><td>").append(resultMap.getOrDefault("bugs", "0")).append("</td></tr>");
        reportBody.append("<tr><td>").append("漏洞").append("</td><td>").append(resultMap.getOrDefault("new_vulnerabilities", "0")).append("</td><td>").append(resultMap.getOrDefault("vulnerabilities", "0")).append("</td></tr>");
        reportBody.append("<tr><td>").append("异味").append("</td><td>").append(resultMap.getOrDefault("new_code_smells", "0")).append("</td><td>").append(resultMap.getOrDefault("code_smells", "0")).append("</td></tr>");
        reportBody.append("<tr><td>").append("覆盖率").append("</td><td>").append(resultMap.getOrDefault("new_coverage", "0.0")).append("%</td><td>").append(resultMap.getOrDefault("coverage", "0.0")).append("%</td></tr>");
        reportBody.append("<tr><td>").append("重复率").append("</td><td>").append(resultMap.getOrDefault("new_duplicated_lines_density", "0.0")).append("%</td><td>").append(resultMap.getOrDefault("duplicated_lines_density", "0.0")).append("%</td></tr>");
        reportBody.append("</table>");
        reportBody.append("<h5>点击链接查看完整报告：<a href=\"").append(fileOutAddress).append("\" target=\"_blank\">").append(fileOutAddress).append("</a></h5>");
        //设置表格样式
        reportBody.append("<style>\n" +
                ".tablex{\n" +
                "    border-spacing: 0;\n" +
                "    border-collapse: collapse;\n" +
                "    width: 70%;\n" +
                "    border: 1px solid #ddd;\n" +
                "}\n" + ".tablex th,.tablex td{\n" +
                "    padding: 7px;\n" +
                "    text-align:center;\n" +
                "    line-height: 24px;\n" +
                "}\n" +
                "</style>");

        createReport(reportBody, param, result);

        result.setQualityData(quality);
    }

    /**
     * 生成产出物报告
     * @param reportBody
     * @param param
     * @param result
     */
    private void createReport(StringBuilder reportBody, AtomParam param, AtomResult result) {
        //拼接报告html
        String reportFileHtml = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><title>SonarQube Scanner</title></head> <body>" + reportBody + "</body> </html>";
        //生成结果目录
        String resultDirPath = param.getBkWorkspace() + "/SonarQubeResult";
        File resultDir = new File(resultDirPath);
        if (!resultDir.exists()) {
            resultDir.mkdirs();
        }
        String filePathName = resultDir.getAbsolutePath() + "/SonarQubeResult-" + param.getServiceName() + ".html";
        File resultFile = new File(filePathName);
        FileWriter fileWriter = null;
        try {
            if (!resultFile.exists()) {
                boolean flag = resultFile.createNewFile();
                logger.info("create " + filePathName + " " + flag);
            }
            fileWriter = new FileWriter(resultFile);
            fileWriter.write(reportFileHtml);
            fileWriter.flush();
        } catch (Exception e) {
            result.setStatus(Status.failure);
            result.setMessage(e.getMessage());
            return;
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    result.setStatus(Status.failure);
                    result.setMessage(e.getMessage());
                }
            }
        }
        Map<String, DataField> data = result.getData();
        //产出物报告;label:报告别名，用于产出物报告界面标识当前报告;path:报告目录所在路径;target:报告入口文件
        ReportData reportData = new ReportData("SonarQubeScanner", resultDirPath, "SonarQubeResult-" + param.getServiceName() + ".html");
        data.put("SonarQubeScanner", reportData);
        result.setData(data);
    }
}
