package com.tencent.bk.devops.atom.pojo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Result<T> {
    public T data; //数据对象
    private int status; //状态码，0代表成功
    private String message; //描述信息

    public Result() {
    }

    public Result(T data) {
        this.status = 0;
        this.data = data;
    }

    public Result(int status, String message) {
        this.status = status;
        this.message = message;
        this.data = null;
    }

    @Override
    public String toString() {
        return "Result{" +
            "status=" + status +
            ", message='" + message + '\'' +
            ", data=" + data +
            '}';
    }

    public boolean isNotOk() {
        return this.status != 0;
    }

    public boolean isOk() {
        return this.status == 0;
    }
}
