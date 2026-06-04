package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.event.ArticleCreatedEvent;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleUpsertService {

  private final ArticleRepository articleRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final NewsCollectMetrics newsCollectMetrics;

  // 출처별 기사 목록을 한 번의 SELECT + saveAll로 일괄 처리해 DB 왕복 비용을 최소화
  @Transactional
  public void upsertAll(ArticleSource source, List<ArticleCandidate> candidates) {
    // null/blank 제거 후 동일 URL 중복 제거 (first-seen 우선, LinkedHashMap으로 순서 보존)
    Map<String, ArticleCandidate> deduped = candidates.stream()
        .filter(c -> c.sourceUrl() != null && !c.sourceUrl().isBlank())
        .collect(Collectors.toMap(
            ArticleCandidate::sourceUrl,
            Function.identity(),
            (a, b) -> a,
            LinkedHashMap::new));

    if (deduped.isEmpty()) {
      return;
    }

    Map<String, Article> existing = articleRepository.findBySourceUrlIn(new ArrayList<>(deduped.keySet())).stream()
        .collect(Collectors.toMap(Article::getSourceUrl, Function.identity(), (a, b) -> a));

    List<Article> toCreate = new ArrayList<>();
    for (ArticleCandidate c : deduped.values()) {
      Article article = existing.get(c.sourceUrl());
      if (article == null) {
        toCreate.add(Article.create(source, c.sourceUrl(), c.title(), c.publishDate(), c.summary()));
      } else if (!article.isDeleted()) {
        article.update(c.title(), c.summary());
        newsCollectMetrics.countDuplicated();
      }
    }

    if (toCreate.isEmpty()) {
      return;
    }
    List<Article> saved = articleRepository.saveAll(toCreate);
    saved.forEach(a -> {
      eventPublisher.publishEvent(new ArticleCreatedEvent(a));
      newsCollectMetrics.countCreated();
    });
  }

}
