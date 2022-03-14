package com.tencent.bk.devops.atom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.devops.atom.pojo.AtomBaseParam;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.utils.http.SdkUtils;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 插件上下文
 *
 * @version 1.0
 */
@SuppressWarnings({"unused"})
public class AtomContext<T extends AtomBaseParam> {

    private static final String ATOM_FILE_ENCODING = "UTF-8";
    private final static Logger logger = LoggerFactory.getLogger(AtomContext.class);
    private final String dataDir;
    private final String inputFile;
    private final String outputFile;
    private final T param;
    private final AtomResult result;
    private final Map<String, String> callBackMap = new ConcurrentHashMap<>();

    /**
     * 插件定义的参数类
     *
     * @param paramClazz 参数类
     * @throws IOException 如果环境问题导致读不到参数类
     */
    public AtomContext(Class<T> paramClazz) throws IOException {
        dataDir = SdkUtils.getDataDir();
        inputFile = SdkUtils.getInputFile();
        outputFile = SdkUtils.getOutputFile();
        param = readParam(paramClazz);
        result = new AtomResult();
    }

    /**
     * 读取请求参数
     *
     * @return 请求参数
     */
    public T getParam() {
        return param;
    }

    /**
     * 获取敏感信息参数
     *
     * @param filedName 字段名
     * @return 敏感信息参数
     */
    public String getSensitiveConfParam(String filedName) {
        Map<String, String> bkSensitiveConfInfo = param.getBkSensitiveConfInfo();
        if (null != bkSensitiveConfInfo) {
            return bkSensitiveConfInfo.get(filedName);
        } else {
            return null;
        }
    }

    /**
     * 根据name 获取回调服务的唯一标识
     *
     * @param name 回调服务的名称
     * @return 回调服务唯一标识
     */
    public String getCallBackUniqueKey(String name) {
        return callBackMap.get(name);
    }

    /**
     * 设置回调服务的唯一标识
     *
     * @param name      回调服务的名称
     * @param uniqueKey 回调服务的唯一标识
     */
    public void setCallBackUniqueKey(String name, String uniqueKey) {
        callBackMap.put(name, uniqueKey);
    }

    /**
     * 获取结果对象
     *
     * @return 结果对象
     */
    @SuppressWarnings({"all"})
    public AtomResult getResult() {
        return result;
    }

    private T readParam(Class<T> paramClazz) throws IOException {
        String json = FileUtils.readFileToString(new File(dataDir + "/" + inputFile), ATOM_FILE_ENCODING);
        return JsonUtil.fromJson(json, paramClazz);
    }

    public Map<String, Object> getAllParameters() throws IOException {
        String json = FileUtils.readFileToString(new File(dataDir + "/" + inputFile), ATOM_FILE_ENCODING);
        return JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {
        });
    }

    void persistent() throws IOException {
        String json = JsonUtil.toJson(result);
        FileUtils.write(new File(dataDir + "/" + outputFile), json, ATOM_FILE_ENCODING);
    }
}
