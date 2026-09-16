package com.analoggpixel.nowinwiki.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.teaopenapi.models.Config;
import com.analoggpixel.nowinwiki.common.exception.BusinessException;
import com.analoggpixel.nowinwiki.config.SmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "nowinwiki.sms.mode", havingValue = "aliyun")
public class AliyunSmsSender implements SmsSender {

    private final SmsProperties smsProperties;

    public AliyunSmsSender(SmsProperties smsProperties) {
        this.smsProperties = smsProperties;
    }

    @Override
    public void sendLoginCode(String phone, String code) {
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();
        try {
            Config config = new Config()
                    .setAccessKeyId(aliyun.getAccessKeyId())
                    .setAccessKeySecret(aliyun.getAccessKeySecret())
                    .setEndpoint(aliyun.getEndpoint());
            Client client = new Client(config);
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(aliyun.getSignName())
                    .setTemplateCode(aliyun.getTemplateCode())
                    .setTemplateParam("{\"code\":\"" + code + "\"}");
            var response = client.sendSms(request);
            if (!"OK".equalsIgnoreCase(response.getBody().getCode())) {
                throw new BusinessException(
                        "短信发送失败：" + response.getBody().getMessage()
                );
            }
            log.info("[aliyun-sms] phone={} bizId={}", phone, response.getBody().getBizId());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[aliyun-sms] send failed phone={}", phone, ex);
            throw new BusinessException("短信发送失败，请稍后重试");
        }
    }
}
