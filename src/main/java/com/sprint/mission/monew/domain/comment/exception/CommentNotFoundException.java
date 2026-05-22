package com.sprint.mission.monew.domain.comment.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class CommentNotFoundException extends CommentException {

  private CommentNotFoundException(Map<String, Object> details) {
    super(ErrorCode.COMMENT_NOT_FOUND, details);
  }

  public static CommentNotFoundException withId(UUID commentId) {
    return new CommentNotFoundException(Map.of("commentId", commentId));
  }
}