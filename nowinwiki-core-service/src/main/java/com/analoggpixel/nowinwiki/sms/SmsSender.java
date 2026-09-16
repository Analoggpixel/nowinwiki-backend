package com.analoggpixel.nowinwiki.sms;

public interface SmsSender {

    void sendLoginCode(String phone, String code);
}
