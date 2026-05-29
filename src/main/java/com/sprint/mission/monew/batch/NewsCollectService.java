package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.event.ArticleCreatedEvent;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.external.naver.NaverNewsClient;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.rss.RssNewsParser;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NewsCollectService {

  private final ArticleRepository articleRepository;
  private final NaverNewsClient naverNewsClient;
  private final RssNewsParser rssNewsParser;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void collect() {
    collectNaver();
    collectRss(ArticleSource.HANKYUNG);
    collectRss(ArticleSource.CHOSUN);
    collectRss(ArticleSource.YONHAP);
  }

  private void collectNaver() {
    try {
      List<NaverNewsItem> items = naverNewsClient.fetchNews();
      for (NaverNewsItem item : items) {
        String sourceUrl = item.originallink() != null && !item.originallink().isBlank()
            ? item.originallink() : item.link();
        String title = NaverNewsClient.stripHtml(item.title());
        String summary = NaverNewsClient.stripHtml(item.description());
        upsert(ArticleSource.NAVER, sourceUrl, title,
            NaverNewsClient.parseNaverDate(item.pubDate()), summary);
      }
      log.info("Naver 뉴스 수집 완료: {}건", items.size());
    } catch (Exception e) {
      log.error("Naver 뉴스 수집 실패", e);
    }
  }

  private void collectRss(ArticleSource source) {
    try {
      List<RssArticleDto> items = rssNewsParser.parse(source);
      for (RssArticleDto item : items) {
        upsert(item.source(), item.sourceUrl(), item.title(), item.publishDate(), item.summary());
      }
      log.info("{} RSS 수집 완료: {}건", source, items.size());
    } catch (Exception e) {
      log.error("{} RSS 수집 실패", source, e);
    }
  }

  private void upsert(ArticleSource source, String sourceUrl, String title,
      Instant publishDate, String summary) {
    if (sourceUrl == null || sourceUrl.isBlank()) {
      log.warn("sourceUrl이 없어 기사를 건너뜁니다: title={}", title);
      return;
    }
    articleRepository.findBySourceUrl(sourceUrl)
        .ifPresentOrElse(
            existing -> existing.update(title, summary),
            () -> {
              Article saved = articleRepository.save(
                  Article.create(source, sourceUrl, title, publishDate, summary));
              eventPublisher.publishEvent(new ArticleCreatedEvent(saved));
            });
  }
}
