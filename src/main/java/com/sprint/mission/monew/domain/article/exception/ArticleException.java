package com.sprint.mission.monew.domain.article.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import com.sprint.mission.monew.common.exception.MonewException;
import java.util.Map;

public abstract class ArticleException extends MonewException {

  protected ArticleException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}