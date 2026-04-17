package com.chatterai.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP 기반 온라인 상태 컨트롤러.
 * 클라이언트는 30초마다 heartbeat를 전송해 presence TTL을 갱신한다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    /**
     * 하트비트 수신 → 온라인 TTL 갱신.
     * /app/presence/heartbeat
     */
    @MessageMapping("/presence/heartbeat")
    public void heartbeat(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        presenceService.setOnline(userId);
        log.debug("Heartbeat received: userId={}", userId);
    }

    /**
     * 자리비움 전환.
     * /app/presence/away
     */
    @MessageMapping("/presence/away")
    public void away(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        presenceService.setAway(userId);
        log.debug("Away set: userId={}", userId);
    }
}
