package com.analoggpixel.nowinwiki.parameter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Boolean success;
    private String errorMsg;
    private T data;

    public static <T> Result<T> ok() {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        return result;
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(String errorMsg) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setErrorMsg(errorMsg);
        return result;
    }
}
