package com.sprint.mission.monew.common.exception;

import java.util.Map;
import lombok.Getter;

@Getter
public abstract class MonewException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  protected MonewException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.details = details;
  }
}