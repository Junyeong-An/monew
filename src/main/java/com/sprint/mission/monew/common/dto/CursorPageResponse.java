package com.sprint.mission.monew.common.dto;

import java.time.Instant;
import java.util.List;

public record CursorPageResponse<T>(
    List<T> content,
    String nextCursor,
    Instant nextAfter,
    boolean hasNext,
    int size,
    Long totalElements) {

  public static <T> CursorPageResponse<T> of(
      List<T> content,
      String nextCursor,
      Instant nextAfter,
      boolean hasNext,
      int size,
      Long totalElements) {
    return new CursorPageResponse<>(content, nextCursor, nextAfter, hasNext, size, totalElements);
  }
}