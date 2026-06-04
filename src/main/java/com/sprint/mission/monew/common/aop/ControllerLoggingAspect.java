package com.sprint.mission.monew.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ControllerLoggingAspect {

  private final RequestUserIdExtractor requestUserIdExtractor;
  private final HttpServletRequest request;

  @Around("execution(* com.sprint.mission.monew.domain..controller..*(..))")
  public Object log(ProceedingJoinPoint jp) throws Throwable {
    String userId = requestUserIdExtractor.extract(request);
    log.debug("요청 수신 | userId={}", userId);

    long start = System.currentTimeMillis();
    try {
      Object result = jp.proceed();
      long elapsed = System.currentTimeMillis() - start;
      int status = result instanceof ResponseEntity<?> re
          ? re.getStatusCode().value() : 200;
      log.info("요청 처리 완료 | status={}, elapsedMs={}", status, elapsed);
      return result;
    } catch (Throwable t) {
      log.warn("요청 처리 실패 | elapsedMs={}", System.currentTimeMillis() - start);
      throw t;
    }
  }
}
