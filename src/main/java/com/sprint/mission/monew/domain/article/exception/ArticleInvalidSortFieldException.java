package com.sprint.mission.monew.domain.article.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;

public class ArticleInvalidSortFieldException extends ArticleException {

  private ArticleInvalidSortFieldException(Map<String, Object> details) {
    super(ErrorCode.ARTICLE_INVALID_SORT_FIELD, details);
  }

  public static ArticleInvalidSortFieldException withOrderBy(String orderBy) {
    return new ArticleInvalidSortFieldException(Map.of("orderBy", orderBy));
  }
}
