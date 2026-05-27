package com.sprint.mission.monew.domain.article.dto;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleSearchRequest(
    String keyword,
    UUID interestId,
    List<ArticleSource> sourceIn,
    Instant publishDateFrom,
    Instant publishDateTo,
    String orderBy,
    String direction,
    String cursor,
    Instant after,
    int limit) {}
