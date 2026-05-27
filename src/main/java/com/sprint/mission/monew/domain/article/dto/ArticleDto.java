package com.sprint.mission.monew.domain.article.dto;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record ArticleDto(
    UUID id,
    ArticleSource source,
    String sourceUrl,
    String title,
    Instant publishDate,
    String summary,
    long commentCount,
    long viewCount,
    boolean viewedByMe) {}
