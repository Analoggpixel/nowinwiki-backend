package com.analoggpixel.nowinwiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.analoggpixel.nowinwiki",
        "com.analoggpixel.nowinwiki.common"
})
public class NowInWikiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NowInWikiApplication.class, args);
    }
}
