package com.tencent.bk.devops.atom.api;


import com.google.common.collect.Maps;
import com.tencent.bk.devops.atom.exception.RemoteServiceException;
import com.tencent.bk.devops.atom.utils.http.OkHttpUtils;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class BaseApi {

    protected static final MediaType JSON_CONTENT_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final Logger logger = LoggerFactory.getLogger(BaseApi.class);
    private final OkHttpClient okHttpClient = OkHttpUtils.createLongClient();

    /**
     * request请求，返回json格式响应报文
     *
     * @param request      request对象
     * @param errorMessage 请求错误信息
     * @return json格式响应报文
     */
    protected String request(Request request, String errorMessage) throws IOException {
        OkHttpClient httpClient = okHttpClient.newBuilder().build();
        try (Response response = httpClient.newCall(request).execute()) {
            String responseContent = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                logger.error("Fail to request(" + request + ") with code " + response.code()
                    + " , message " + response.message() + " and response" + responseContent);
                logger.info("excep>>>>>>>>>>>>" + response);
                throw new RemoteServiceException(errorMessage, response.code(), responseContent);
            }
            return responseContent;
        }
    }

    /**
     * get请求，返回request对象
     *
     * @param path    请求路径
     * @param headers 请求头
     * @return request对象
     */
    public Request buildGet(String path, Map<String, String> headers) {
        String url = buildUrl(path);
        return new Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).get().build();
    }

    /**
     * get请求，返回request对象
     *
     * @param path 请求路径
     * @return request对象
     */
    public Request buildGet(String path) {
        return buildGet(path, Maps.newHashMap());
    }

    /**
     * post请求，返回request对象
     *
     * @param path 请求路径
     * @return request对象
     */
    public Request buildPost(String path) {
        return buildPost(path, Maps.newHashMap());
    }

    /**
     * post请求，返回request对象
     *
     * @param path    请求路径
     * @param headers 请求头
     * @return request对象
     */
    public Request buildPost(String path, Map<String, String> headers) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "");
        return buildPost(path, requestBody, headers);
    }

    /**
     * post请求，返回request对象
     *
     * @param path        请求路径
     * @param requestBody 请求报文体
     * @param headers     请求头
     * @return request对象
     */
    public Request buildPost(String path, RequestBody requestBody, Map<String, String> headers) {
        String url = buildUrl(path);
        return new Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).post(requestBody).build();
    }

    /**
     * put请求，返回request对象
     *
     * @param path 请求路径
     * @return request对象
     */
    public Request buildPut(String path) {
        return buildPut(path, Maps.newHashMap());
    }

    /**
     * put请求，返回request对象
     *
     * @param path    请求路径
     * @param headers 请求头
     * @return request对象
     */
    public Request buildPut(String path, Map<String, String> headers) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "");
        return buildPut(path, requestBody, headers);
    }

    /**
     * post请求，返回request对象
     *
     * @param path        请求路径
     * @param requestBody 请求报文体
     * @param headers     请求头
     * @return request对象
     */
    public Request buildPut(String path, RequestBody requestBody, Map<String, String> headers) {
        String url = buildUrl(path);
        return new Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).put(requestBody).build();
    }

    /**
     * delete请求，返回request对象
     *
     * @param path    请求路径
     * @param headers 请求头
     */
    public Request buildDelete(String path, Map<String, String> headers) {
        String url = buildUrl(path);
        return new Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).delete().build();
    }

    /**
     * delete请求，返回request对象
     *
     * @param path        请求路径
     * @param requestBody 请求体
     * @param headers     请求头
     * @return request对象
     */
    public Request buildDelete(String path, RequestBody requestBody, Map<String, String> headers) {
        String url = buildUrl(path);
        return new Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).delete(requestBody).build();
    }

    /**
     * 生成json形式请求报文体，返回请求报文体
     *
     * @param data 请求数据对象
     * @return json形式请求报文体
     */
    public RequestBody getJsonRequest(Object data) {
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(data));
    }

    public String encode(String parameter) throws UnsupportedEncodingException {
        return URLEncoder.encode(parameter, "UTF-8");
    }

    private String buildUrl(String path) {
        return SdkEnv.genUrl(path);
    }

    private Map<String, String> getAllHeaders(Map<String, String> headers) {
        headers.putAll(SdkEnv.getSdkHeader());
        return headers;
    }

}
