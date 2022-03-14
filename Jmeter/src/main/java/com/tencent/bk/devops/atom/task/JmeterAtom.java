package com.tencent.bk.devops.atom.task;

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
import com.tencent.bk.devops.atom.task.utils.JFrogUtil;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Jmeter
 */
@AtomService(paramClass = AtomParam.class)
public class JmeterAtom implements TaskAtom<AtomParam> {

    private final static Logger logger = LoggerFactory.getLogger(JmeterAtom.class);

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
        //默认./
        String jmxPath = param.getPath();

        // 3. 处理插件业务逻辑
        // jmx文件路径处理
        if (StringUtils.isEmpty(jmxPath)) {
            jmxPath = "/";
        } else {
            jmxPath = jmxPath.replace(".", "");
            if (!"/".equals(jmxPath.substring(0, 1))) {
                jmxPath = "/" + jmxPath;
            }
        }
        logger.info("jmxPath:" + jmxPath);

        //筛选jmxFile

        ArrayList<String> jmxFileList = new ArrayList<>();
        getJmxFilePath(param.getBkWorkspace() + jmxPath, jmxFileList, result);
        if (jmxFileList.size() == 0) {
            result.setStatus(Status.failure);
            result.setMessage("未找到.jmx后缀文件！");
            return;
        }
        logger.info("jmxFileList:" + jmxFileList);

        //测试报告内容，产出物报告内容
        StringBuilder testReportContent = new StringBuilder();
        //循环执行jmeter测试计划文件并拼接results file内容
        logger.info("jmeter start");
        int passNum = 0;
        int totalNum = 0;
        for (String jmxFilePath : jmxFileList) {
            int lastIndexOfPoint = jmxFilePath.lastIndexOf(".");
            String fileName = lastIndexOfPoint > -1 ?
                jmxFilePath.substring(0, lastIndexOfPoint).replace('/', '-').replace('\\', '-')
                : jmxFilePath.replace('/', '-').replace('\\', '-');
            // 这里没考虑文件重名问题
//            String[] pathArr = jmxFilePath.split("/");
//            String fileNameSuffix = pathArr[pathArr.length - 1];
//            String fileName = fileNameSuffix.split("\\.")[0];
            String command = "jmeter -n -t " + jmxFilePath + " -l " + fileName + ".jtl";
            logger.info("jmeter test " + fileName + " command:" + command);
            logger.info("start test " + jmxFilePath);

            String jmeterLog = CommandLineUtils.execute(command, new File(param.getBkWorkspace() + jmxPath), false, "");
            logger.info("jmeterLog:\n" + jmeterLog);
            logger.info("end test " + jmxFilePath);
//            logger.info(jmxFilePath + " 测试完成，结果可在产出物报告查看！");

            //results file路径
            String logFile = param.getBkWorkspace() + jmxPath + fileName + ".jtl";
            testReportContent.append("<h3>").append(fileName).append(".jtl").append("</h3>");
            try {
                //读当前测试计划生成的results file xxx.csv
                BufferedReader reader = new BufferedReader(new FileReader(logFile));
                //results file xxx.csv内容转换成表格
                testReportContent.append("<table class='tablex' border='1'><tr>");
                //读取results file xxx.csv头信息：timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect
                String tittleLine = reader.readLine();
                String[] tittleArr = tittleLine.split(",");
                int successIndex = -1;
                for (int i = 0; i < tittleArr.length; i++) {
                    String tittle = tittleArr[i];
                    testReportContent.append("<th>").append(tittle).append("</th>");
                    if ("success".equals(tittle.trim())) {
                        successIndex = i;
                    }
                }
                testReportContent.append("</tr>");
                //读results file xxx.csv 结果内容
                String contentLine;
                while ((contentLine = reader.readLine()) != null) {
                    totalNum++;
                    String[] contentLineArr = contentLine.split(",");
                    testReportContent.append("<tr>");
                    for (int i = 0; i < tittleArr.length; i++) {
                        String tdContent = contentLineArr[i];
                        testReportContent.append("<td>").append(tdContent).append("</td>");
                        if (i == successIndex && Boolean.getBoolean(tdContent.trim())) {
                            passNum++;
                        }
                    }
                    testReportContent.append("</tr>");
                }
                testReportContent.append("</table>");
            } catch (Exception e) {
                result.setStatus(Status.failure);
                result.setMessage(e.getMessage());
            }
        }
        logger.info("jmeter end");

        //设置表格样式
        testReportContent.append("<style>\n" +
            ".tablex{\n" +
            "    border-spacing: 0;\n" +
            "    border-collapse: collapse;\n" +
            "    width: 100%;\n" +
            "    border: 1px solid #ddd;\n" +
            "}\n" + ".tablex th,.tablex td{\n" +
            "    padding: 7px;\n" +
            "    line-height: 24px;\n" +
            "}\n" +
            "</style>");

        //String elementId = "jmeter";
        String elementId = param.getPipelineTaskId();
        String relativePath = "surefire-report.html";
        String reportPath = JFrogUtil.getReportPath(param.getProjectName(), param.getPipelineId(), param.getPipelineBuildId(), elementId, relativePath);
        logger.info("ReportPath:" + reportPath);

        //拼接报告html
        String reportFileHtml = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><title>jmeter</title></head> <body>" + testReportContent + "</body> </html>";
        //生成结果目录
        String resultDirPath = param.getBkWorkspace() + "/jmeterResult";
        File resultDir = new File(resultDirPath);
        if (!resultDir.exists()) {
            resultDir.mkdirs();
        }
        String filePathName = resultDir.getAbsolutePath() + "/jmeterResult.html";
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
        result.setStatus(Status.success);
        result.setMessage("Jmeter执行成功！");
        Map<String, DataField> data = result.getData();
        //产出物报告;label:报告别名，用于产出物报告界面标识当前报告;path:报告目录所在路径;target:报告入口文件
        ReportData reportData = new ReportData("Jmeter", resultDirPath, "jmeterResult.html");
        data.put("Jmeter", reportData);
        result.setData(data);
        float passRate = 0;
        if (totalNum > 0) {
            passRate = ((float) passNum) / ((float) totalNum);
        }

        logger.info("test case pass rate is : " + passRate);
        result.setType("quality");
        Map<String, QualityValue> quality = new HashMap<>();
        quality.put("passRate", new QualityValue("" + passRate));
        result.setQualityData(quality);
    }

    private void getJmxFilePath(String basicPath, ArrayList<String> result, AtomResult atomResult) {
        File textFileDir = new File(basicPath);
        if (!textFileDir.exists()) {
            atomResult.setStatus(Status.failure);
            atomResult.setMessage("目录不存在: " + basicPath);
        }
        File[] fileArr = textFileDir.listFiles();
        if (fileArr != null && fileArr.length > 0) {
            for (File file : fileArr) {
                String filePath = file.getAbsolutePath();
                if (file.isDirectory()) {
                    getJmxFilePath(filePath, result, atomResult);
                } else if (file.isFile() && filePath.endsWith(".jmx")) {
                    result.add(filePath);
                }
            }
        }
    }

}
