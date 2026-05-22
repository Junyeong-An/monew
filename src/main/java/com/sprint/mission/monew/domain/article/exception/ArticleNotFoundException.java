package com.sprint.mission.monew.domain.article.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class ArticleNotFoundException extends ArticleException {

  private ArticleNotFoundException(Map<String, Object> details) {
    super(ErrorCode.ARTICLE_NOT_FOUND, details);
  }

  public static ArticleNotFoundException withId(UUID articleId) {
    return new ArticleNotFoundException(Map.of("articleId", articleId));
  }
}