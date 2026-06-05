package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.common.exception.InternalErrorCode;
import com.sprint.mission.monew.common.exception.MonewInternalException;

public class ArticleBackupFailedException extends MonewInternalException {

  private ArticleBackupFailedException(String s3Key, Throwable cause) {
    super(InternalErrorCode.ARTICLE_BACKUP_FAILED, s3Key, cause);
  }

  public static ArticleBackupFailedException withKey(String s3Key, Throwable cause) {
    return new ArticleBackupFailedException(s3Key, cause);
  }
}
