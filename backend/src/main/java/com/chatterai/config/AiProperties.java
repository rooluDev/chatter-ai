package com.chatterai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private int dailyUsageLimit = 20;
    private int timeoutSeconds = 10;
    private int contextMessageCount = 10;
    private Claude claude = new Claude();

    @Getter
    @Setter
    public static class Claude {
        private String apiKey;
        private String model = "claude-sonnet-4-5";
    }
}
