package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.event.ArticleCreatedEvent;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.util.ArrayList;
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
    if (candidates.isEmpty()) {
      return;
    }
    List<String> urls = candidates.stream().map(ArticleCandidate::sourceUrl).toList();
    Map<String, Article> existing = articleRepository.findBySourceUrlIn(urls).stream()
        .collect(Collectors.toMap(Article::getSourceUrl, Function.identity(), (a, b) -> a));

    List<Article> toCreate = new ArrayList<>();
    for (ArticleCandidate c : candidates) {
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
