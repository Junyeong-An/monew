package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import com.sprint.mission.monew.common.exception.MonewException;
import java.util.Map;

public abstract class UserException extends MonewException {

  protected UserException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}