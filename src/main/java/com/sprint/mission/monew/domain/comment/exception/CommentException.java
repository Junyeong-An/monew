package com.sprint.mission.monew.domain.comment.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import com.sprint.mission.monew.common.exception.MonewException;
import java.util.Map;

public abstract class CommentException extends MonewException {

  protected CommentException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}