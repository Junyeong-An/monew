package com.sprint.mission.monew.domain.article.controller;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.article.controller.api.ArticleApi;
import com.sprint.mission.monew.domain.article.dto.ArticleDto;
import com.sprint.mission.monew.domain.article.dto.ArticleSearchRequest;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.service.ArticleService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController implements ArticleApi {

  private final ArticleService articleService;

  @GetMapping
  @Override
  public ResponseEntity<CursorPageResponse<ArticleDto>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) UUID interestId,
      @RequestParam(required = false) List<ArticleSource> sourceIn,
      @RequestParam(required = false) Instant publishDateFrom,
      @RequestParam(required = false) Instant publishDateTo,
      @RequestParam String orderBy,
      @RequestParam String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam int limit,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    ArticleSearchRequest request =
        new ArticleSearchRequest(
            keyword,
            interestId,
            sourceIn,
            publishDateFrom,
            publishDateTo,
            orderBy,
            direction,
            cursor,
            after,
            limit);
    return ResponseEntity.ok(articleService.search(request, requestUserId));
  }
}
