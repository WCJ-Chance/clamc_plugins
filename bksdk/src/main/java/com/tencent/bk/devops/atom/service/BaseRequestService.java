package com.tencent.bk.devops.atom.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.devops.atom.utils.http.OkHttpUtils;
import lombok.Data;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class BaseRequestService {
    private final static Logger logger = LoggerFactory.getLogger(BaseRequestService.class);
    protected String baseUrl = "http://devops.clamc.com";
//    protected String baseUrl = "http://devops.bktest.com";
    protected String projectName;
    protected String cookie;


    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected String updateCookie(Response response) {
        if (response != null) {
            String cookies = response.headers("Set-Cookie")
                    .stream()
                    .map(c -> c.split(";")[0])
                    .collect(Collectors.joining(";"));
            if (StringUtils.isNotEmpty(cookies)) {
                this.cookie = cookies;
            } else {
                logger.debug("服务端未返回cookie信息");
                logger.debug(response.toString());
            }
        } else {
            logger.error("更新Cookie失败！");
            this.cookie = null;
        }
        return this.cookie;
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
    /**
     * 原始Get请求
     * @param url 请求地址
     * @param headers 自定义Headers
     * @return 原始响应
     */
    public Response doGetRaw(String url, Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Cookie", cookie);
        Request.Builder builder = new Request.Builder();
        Request req = builder.url(url).headers(Headers.of(headers))
                .get().build();
        Response response = OkHttpUtils.doHttpRaw(req);
        updateCookie(response);
        return response;
    }
    /**
     * 原始Post请求
     * @param url 请求地址
     * @param jsonData 请求的Json串，String格式
     * @param headers 附加的Headers
     * @return 原始响应的Response
     */
    public Response doPostRaw(String url, String jsonData, Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        if (jsonData == null) {
            jsonData = "";
        }
        headers.put("Cookie", cookie);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        Request.Builder builder = new Request.Builder();
        Request req = builder.url(url)
                .post(RequestBody.create(jsonData, JSON))
                .headers(Headers.of(headers))
                .build();
        Response response = OkHttpUtils.doHttpRaw(req);
        updateCookie(response);
        return response;
    }

    /**
     * 原始Patch请求
     * @param url 请求地址
     * @param jsonData 请求的Json串，String格式
     * @param headers 附加的Headers
     * @return 原始响应的Response
     */
    public Response doPatchRaw(String url, String jsonData, Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Cookie", cookie);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        Request.Builder builder = new Request.Builder();
        Request req = builder.url(url)
                .patch(RequestBody.create(jsonData, JSON))
                .headers(Headers.of(headers))
                .build();
        Response response = OkHttpUtils.doHttpRaw(req);
        updateCookie(response);
        return response;
    }

    /**
     * 原始Put请求
     * @param url 请求地址
     * @param jsonData 请求的Json串，String格式
     * @param headers 附加的Headers
     * @return 原始响应的Response
     */
    public Response doPutRaw(String url, String jsonData, Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Cookie", cookie);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        Request.Builder builder = new Request.Builder();
        Request req = builder.url(url)
                .put(RequestBody.create(jsonData, JSON))
                .headers(Headers.of(headers))
                .build();
        Response response = OkHttpUtils.doHttpRaw(req);
        updateCookie(response);
        return response;
    }

    /**
     * 原始Delete请求
     * @param url 请求地址
     * @param headers 自定义Headers
     * @return 原始响应
     */
    public Response doDeleteRaw(String url, Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Cookie", cookie);
        Request.Builder builder = new Request.Builder();
        Request req = builder.url(url).headers(Headers.of(headers))
                .delete().build();
        Response response = OkHttpUtils.doHttpRaw(req);
        updateCookie(response);
        return response;
    }
}
