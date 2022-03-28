package com.tencent.bk.devops.atom.task.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.devops.atom.api.BaseApi;
import com.tencent.bk.devops.atom.pojo.Result;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public class CredentialApi extends BaseApi {

    private final static Logger logger = LoggerFactory.getLogger(CredentialApi.class);

    /**
     * 获取凭证信息
     *
     * @param credentialId 凭证ID
     * @return
     */
    @SuppressWarnings("all")
    public Result<Map<String, String>> getCredential(String credentialId, String publicKey) throws UnsupportedEncodingException {
        Request request = super.buildGet("/ms/ticket/api/build/credentials/" + credentialId + "?publicKey=" + encode(publicKey));
        String responseContent = null;
        try {
            responseContent = super.request(request, "获取凭证信息失败");
        } catch (IOException e) {
            logger.error("getCredential throw Exception", e);
        }
        if (null != responseContent) {
            return JsonUtil.fromJson(responseContent, new TypeReference<Result<Map<String, String>>>() {
            });
        } else {
            return new Result(null);
        }
    }

    public Result<Map<String, String>> getCredential(String credentialId) {
        Request request = super.buildGet("/ticket/api/build/credentials/" + credentialId + "/detail");
        String responseContent = null;
        try {
            responseContent = super.request(request, "获取凭证信息失败");
        } catch (IOException e) {
            logger.error("getCredential throw Exception", e);
        }
        if (null != responseContent) {
            return JsonUtil.fromJson(responseContent, new TypeReference<Result<Map<String, String>>>() {
            });
        } else {
            return new Result(null);
        }
    }

}