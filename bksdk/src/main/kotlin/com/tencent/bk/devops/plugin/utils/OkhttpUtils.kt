package com.tencent.bk.devops.plugin.utils

import okhttp3.ConnectionPool
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

object OkhttpUtils {

    private val logger = LoggerFactory.getLogger(OkhttpUtils::class.java)

    private val trustAllCerts = arrayOf<X509TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}

        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}

        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    })
    val sslSocketFactory: SSLSocketFactory

    init {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        sslSocketFactory = sslContext.socketFactory
    }

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.MINUTES)
        .writeTimeout(30L, TimeUnit.MINUTES)
        .connectionPool(ConnectionPool(64, 5, TimeUnit.MINUTES))
        .hostnameVerifier(HostnameVerifier { _, _ -> true })
        .sslSocketFactory(sslSocketFactory, trustAllCerts[0])
        .build()

    private val shortOkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.SECONDS)
        .writeTimeout(30L, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(64, 5, TimeUnit.MINUTES))
        .sslSocketFactory(sslSocketFactory, trustAllCerts[0])
        .hostnameVerifier(HostnameVerifier { _, _ -> true })
        .build()

    private val noRetryShortOkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(5L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.SECONDS)
        .writeTimeout(30L, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(64, 5, TimeUnit.MINUTES))
        .sslSocketFactory(sslSocketFactory, trustAllCerts[0])
        .hostnameVerifier(HostnameVerifier { _, _ -> true })
        .retryOnConnectionFailure(false)
        .build()

    private val noRetryOkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.MINUTES)
        .writeTimeout(30L, TimeUnit.MINUTES)
        .connectionPool(ConnectionPool(64, 5, TimeUnit.MINUTES))
        .hostnameVerifier(HostnameVerifier { _, _ -> true })
        .sslSocketFactory(sslSocketFactory, trustAllCerts[0])
        .retryOnConnectionFailure(false)
        .build()

    private const val CONTENT_TYPE_JSON = "application/json; charset=utf-8"

    fun doShortGet(url: String, headers: Map<String, String> = mapOf()): Response {
        return doGet(shortOkHttpClient, url, headers)
    }

    fun doGet(url: String, headers: Map<String, String> = mapOf()): Response {
        return doGet(okHttpClient, url, headers)
    }

    fun doGet(client: OkHttpClient, url: String, headers: Map<String, String> = mapOf()): Response {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        return client.newCall(request).execute()
    }

    fun doShortHttp(request: Request): Response {
        return shortOkHttpClient.newCall(request).execute()
    }

    fun doShortHttpNoRetry(request: Request): Response {
        return noRetryShortOkHttpClient.newCall(request).execute()
    }

    fun doHttp(request: Request): Response {
        return okHttpClient.newCall(request).execute()
    }

    fun doHttpNoRetry(request: Request): Response {
        return noRetryOkHttpClient.newCall(request).execute()
    }

    fun downloadFile(url: String, destPath: File) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == 404) {
                logger.warn("The file $url is not exist")
                throw RuntimeException("文件不存在")
            }
            if (!response.isSuccessful) {
                logger.warn("fail to download the file from $url " +
                    "because of ${response.message} and code ${response.code}")
                throw RuntimeException("获取文件失败")
            }
            if (!destPath.parentFile.exists()) destPath.parentFile.mkdirs()
            val buf = ByteArray(4096)
            response.body!!.byteStream().use { bs ->
                var len = bs.read(buf)
                FileOutputStream(destPath).use { fos ->
                    while (len != -1) {
                        fos.write(buf, 0, len)
                        len = bs.read(buf)
                    }
                }
            }
        }
    }

    fun downloadFile(response: Response, destPath: File) {
        if (response.code == 304) {
            logger.info("file is newest, do not download to $destPath")
            return
        }
        if (!response.isSuccessful) {
            logger.warn("fail to download the file because of ${response.message} and code ${response.code}")
            throw RuntimeException("获取文件失败")
        }
        if (!destPath.parentFile.exists()) destPath.parentFile.mkdirs()
        val buf = ByteArray(4096)
        response.body!!.byteStream().use { bs ->
            var len = bs.read(buf)
            FileOutputStream(destPath).use { fos ->
                while (len != -1) {
                    fos.write(buf, 0, len)
                    len = bs.read(buf)
                }
            }
        }
    }

    fun doPost(url: String, jsonParam: String, headers: Map<String, String> = mapOf()): Response {
        val builder = getBuilder(url, headers)
        val body = jsonParam.toRequestBody(CONTENT_TYPE_JSON.toMediaTypeOrNull())
        val request = builder.post(body).build()
        return doHttp(request)
    }

    private fun getBuilder(url: String, headers: Map<String, String>?): Request.Builder {
        val builder = Request.Builder()
        builder.url(url)
        if (null != headers) {
            builder.headers(headers.toHeaders())
        }
        return builder
    }
}
