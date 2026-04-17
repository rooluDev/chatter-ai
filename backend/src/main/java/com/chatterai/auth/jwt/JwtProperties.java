package com.chatterai.auth.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiry;   // ms (30분 = 1800000)
    private long refreshTokenExpiry;  // 초 (7일 = 604800, Redis TTL용)
}
