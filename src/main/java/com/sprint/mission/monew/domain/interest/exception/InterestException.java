package com.sprint.mission.monew.domain.interest.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import com.sprint.mission.monew.common.exception.MonewException;
import java.util.Map;

public abstract class InterestException extends MonewException {

  protected InterestException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}