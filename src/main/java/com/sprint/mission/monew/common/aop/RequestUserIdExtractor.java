package com.sprint.mission.monew.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestUserIdExtractor {

  public String extract(HttpServletRequest request) {
    String userId = request.getHeader("Monew-Request-User-ID");
    return userId != null ? userId : "없음";
  }
}
