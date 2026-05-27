package com.sprint.mission.monew.domain.article.service;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleDto;
import com.sprint.mission.monew.domain.article.dto.ArticleSearchRequest;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.mapper.ArticleMapper;
import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import com.sprint.mission.monew.domain.article.repository.querydsl.ArticleQueryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleService {

  private final ArticleQueryRepository articleQueryRepository;
  private final ArticleViewRepository articleViewRepository;
  private final ArticleMapper articleMapper;

  public CursorPageResponse<ArticleDto> search(ArticleSearchRequest request, UUID requestUserId) {
    long totalElements = articleQueryRepository.count(request);
    List<Article> articles = articleQueryRepository.findAll(request);

    boolean hasNext = articles.size() > request.limit();
    List<Article> page = hasNext ? articles.subList(0, request.limit()) : articles;

    List<UUID> articleIds = page.stream().map(Article::getId).collect(Collectors.toList());
    Set<UUID> viewedIds =
        articleIds.isEmpty()
            ? Set.of()
            : articleViewRepository.findArticleIdsByArticleIdsAndUserId(articleIds, requestUserId);

    List<ArticleDto> content =
        page.stream()
            .map(a -> articleMapper.toDto(a, viewedIds.contains(a.getId())))
            .collect(Collectors.toList());

    String nextCursor = null;
    Instant nextAfter = null;
    if (hasNext && !page.isEmpty()) {
      Article last = page.get(page.size() - 1);
      nextCursor = buildCursor(last, request.orderBy());
      nextAfter = last.getCreatedAt();
    }

    return CursorPageResponse.of(content, nextCursor, nextAfter, hasNext, content.size(), totalElements);
  }

  private String buildCursor(Article article, String orderBy) {
    return switch (orderBy) {
      case "publishDate" -> article.getPublishDate().toString();
      case "commentCount" -> String.valueOf(article.getCommentCount());
      case "viewCount" -> String.valueOf(article.getViewCount());
      default -> throw new IllegalStateException("지원하지 않는 정렬 기준: " + orderBy);
    };
  }
}
