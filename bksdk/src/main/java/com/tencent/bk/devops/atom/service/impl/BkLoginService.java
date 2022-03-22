package com.tencent.bk.devops.atom.service.impl;

import com.tencent.bk.devops.atom.service.BaseRequestService;
import lombok.SneakyThrows;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

public class BkLoginService extends BaseRequestService {
    private final static Logger logger = LoggerFactory.getLogger(BkLoginService.class);
    String baseUrl;

    public BkLoginService() {
        this.baseUrl = "http://paas.clamc.com.cn";
    }
    public BkLoginService(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @SneakyThrows
    public static String encrypt(String str, String publicKey) {
        //base64编码的公钥
        byte[] decoded = Base64.decodeBase64(publicKey);
        RSAPublicKey pubKey= (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        //RAS加密
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE,pubKey);
        String outStr=Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
        return outStr;
    }

    /**
     * 登录并获取用户登录态
     * @param username 用户名
     * @param password 密码
     * @return 登陆后的BkToken
     */
    public String login(String username, String password) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            logger.error("用户名或密码未给出");
            return null;
        }
        String csrfToken = "lPitPHNNlDhp167vJkphyAM9UxUJ0e15";
        String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDVLJnXjPFevIDzKzW4kXMgd16a3c+KfKZtuOS3ye15hUWAlkApcO9eSN1TUHwLGEoJxHuLBfkFG2PEGj5vf28H+ZkTdD/zrKXy+LiHduRCEjyyymWoAQANH+4Kq7kSb97tanQ1kwQ0Diobip4ltkBWxBhXbWh6TKu+wGf4WF+NBwIDAQAB";
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .followRedirects(false).build();
        FormBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", encrypt(password, publicKey))
//                .add("password", password)
                .add("csrfmiddlewaretoken", csrfToken)
                .add("next", "")
                .add("app_id", "")
                .build();
        String url = baseUrl + "/login/?c_url=/";

        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", url);
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Cookie", "bklogin_csrftoken=" + csrfToken);
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .headers(Headers.of(headers))
                .build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            return super.updateCookie(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
