package com.apzda.cloud.captcha.error;

import com.apzda.cloud.gsvc.IServiceError;

public record NeedCaptcha(String message) implements IServiceError {
    @Override
    public int code() {
        return 4;
    }
}
