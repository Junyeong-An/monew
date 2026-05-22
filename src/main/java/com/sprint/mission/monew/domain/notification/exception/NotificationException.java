package com.sprint.mission.monew.domain.notification.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import com.sprint.mission.monew.common.exception.MonewException;
import java.util.Map;

public abstract class NotificationException extends MonewException {

  protected NotificationException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}