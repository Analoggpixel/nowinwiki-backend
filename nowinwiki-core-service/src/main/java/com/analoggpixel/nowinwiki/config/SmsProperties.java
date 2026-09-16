package com.analoggpixel.nowinwiki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "nowinwiki.sms")
public class SmsProperties {

    /**
     * mock | aliyun
     */
    private String mode = "mock";

    private Aliyun aliyun = new Aliyun();

    @Data
    public static class Aliyun {
        private String accessKeyId;
        private String accessKeySecret;
        private String signName;
        private String templateCode;
        private String endpoint = "dysmsapi.aliyuncs.com";
    }
}
