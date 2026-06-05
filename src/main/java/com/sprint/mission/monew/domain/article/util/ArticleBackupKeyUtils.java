package com.sprint.mission.monew.domain.article.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class ArticleBackupKeyUtils {

  private static final DateTimeFormatter PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
  private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private ArticleBackupKeyUtils() {}

  public static String s3Key(LocalDate date) {
    return "articles/" + date.format(PATH_FORMATTER)
        + "/articles-" + date.format(FILE_FORMATTER) + ".json.gz";
  }
}
