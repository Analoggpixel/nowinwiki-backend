package com.analoggpixel.nowinwiki.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "nowinwiki.sms.mode", havingValue = "mock", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    @Override
    public void sendLoginCode(String phone, String code) {
        log.info("[mock-sms] phone={} code={}", phone, code);
    }
}
