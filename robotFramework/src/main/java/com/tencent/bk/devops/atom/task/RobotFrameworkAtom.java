package com.tencent.bk.devops.atom.task;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.pojo.quality.QualityValue;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import com.tencent.bk.devops.atom.task.utils.command.CommandLineUtils;
import com.tencent.bk.devops.atom.task.pojo.AtomParam;
import com.tencent.bk.devops.atom.task.utils.dom4j.XmlUtil;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import kotlin.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 */
@AtomService(paramClass = AtomParam.class)
public class RobotFrameworkAtom implements TaskAtom<AtomParam> {

    private final static Logger logger = LoggerFactory.getLogger(RobotFrameworkAtom.class);

    /**
     * 执行主入口
     *
     * @param atomContext 插件上下文
     */
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
        // 3. 模拟处理插件业务逻辑
        String basicPath = param.getBasicPath();
        if (basicPath == null) {
            basicPath = "";
        }
        String searchPath = param.getBkWorkspace() + (basicPath.startsWith("/") ? basicPath : ("/" + basicPath));
        ArrayList<String> txtFiles = new ArrayList<>();
        getTxtFilePath(searchPath, txtFiles, result);
        if (result.getStatus() != Status.success) {
            return;
        }
        // 创建robot-framework检测的输出空间
        String robotFrameworkOutputPath = param.getBkWorkspace() + "/robot-framework-workspace";
        logger.info("robot-framework的输出目录为: " + robotFrameworkOutputPath);
        CommandLineUtils.execute("mkdir " + robotFrameworkOutputPath, new File(param.getBkWorkspace()), "");
        List<String> xmlPaths = new ArrayList<>();
        for (String path : txtFiles) {
            int lastIndexOfPoint = path.lastIndexOf(".");
            String preFileName = path.substring(0, lastIndexOfPoint).replace('/', '-').replace('\\', '-');
            String outputPath = robotFrameworkOutputPath + "/" + preFileName + "-output.xml";
            xmlPaths.add(outputPath);
            String logPath = robotFrameworkOutputPath + "/" + preFileName + "-log.html";
            String reportPath = robotFrameworkOutputPath + "/" + preFileName + "-report.html";
            String cmd = "robot -o " + outputPath + " -l " + logPath + " -r " + reportPath + " " + path;
            logger.info(cmd);
            CommandLineUtils.execute(cmd, new File(param.getBkWorkspace()), "");
        }
        int totalCase = 0;
        int passCase = 0;
        for (String path : xmlPaths) {
            logger.error("xml path is : {}", path);
            Pair<Integer, Integer> testCaseStatus = XmlUtil.getTestCaseStatus(path, result);
            if (testCaseStatus == null) {
                return;
            }
            passCase += testCaseStatus.getFirst();
            totalCase += testCaseStatus.getSecond();
        }
        float passRate = 0;
        if (totalCase > 0) {
            passRate = ((float) passCase) / ((float) totalCase);
        }
        logger.info("test case pass rate is : " + passRate);
        result.setType("quality");
        Map<String, QualityValue> quality = new HashMap<>();
        quality.put("passRate", new QualityValue("" + passRate));
        result.setQualityData(quality);
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

    private void getTxtFilePath(String basicPath, ArrayList<String> result, AtomResult atomResult) {
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
                    getTxtFilePath(filePath, result, atomResult);
                } else if (file.isFile() && filePath.endsWith(".txt")) {
                    result.add(filePath);
                }
            }
        }
    }

//    public static Object exec(String cmd) {
//        try {
//            String[] cmdA = {"/bin/sh", "-c", cmd};
//            Process process = Runtime.getRuntime().exec(cmdA);
//            LineNumberReader br = new LineNumberReader(new InputStreamReader(
//                process.getInputStream()));
//            StringBuffer sb = new StringBuffer();
//            String line;
//            while ((line = br.readLine()) != null) {
//                System.out.println(line);
//                sb.append(line).append("\n");
//            }
//            return sb.toString();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
}
