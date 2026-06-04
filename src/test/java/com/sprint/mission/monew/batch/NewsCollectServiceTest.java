package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.external.naver.NaverNewsClient;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.rss.RssNewsParser;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsCollectServiceTest {

  @InjectMocks NewsCollectService newsCollectService;
  @Mock ArticleUpsertService articleUpsertService;
  @Mock NaverNewsClient naverNewsClient;
  @Mock RssNewsParser rssNewsParser;
  @Mock NewsCollectMetrics newsCollectMetrics;

  @Nested
  @DisplayName("뉴스 수집")
  class Collect {

    @Test
    @DisplayName("Naver 기사를 수집하면 upsertAll에 일괄 위임한다")
    void Naver_기사를_수집하면_upsertAll에_일괄_위임한다() {
      // given
      NaverNewsItem item = new NaverNewsItem(
          "제목", "https://example.com/1", "https://example.com/1",
          "요약", "Mon, 29 May 2026 00:00:00 +0900");
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then
      verify(articleUpsertService).upsertAll(
          eq(ArticleSource.NAVER),
          argThat(list -> list.size() == 1 && list.get(0).sourceUrl().equals("https://example.com/1")));
    }

    @Test
    @DisplayName("RSS 기사를 수집하면 upsertAll에 일괄 위임한다")
    void RSS_기사를_수집하면_upsertAll에_일괄_위임한다() {
      // given
      RssArticleDto rssItem = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");
      given(naverNewsClient.fetchNews()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(rssItem));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then
      verify(articleUpsertService).upsertAll(
          eq(ArticleSource.HANKYUNG),
          argThat(list -> list.size() == 1 && list.get(0).sourceUrl().equals("https://hankyung.com/1")));
    }

    @Test
    @DisplayName("upsertAll 실패 시 출처 수집이 실패해도 다른 출처는 계속 수집한다")
    void upsertAll_실패_시_다른_출처는_계속_수집한다() {
      // given
      RssArticleDto rssItem = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");
      given(naverNewsClient.fetchNews()).willThrow(new RuntimeException("Naver API 오류"));
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(rssItem));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());

      // when & then — 예외 없이 완료, HANKYUNG은 upsertAll 호출됨
      assertThatNoException().isThrownBy(() -> newsCollectService.collect());
      verify(articleUpsertService).upsertAll(eq(ArticleSource.HANKYUNG), anyList());
    }

    @Test
    @DisplayName("RSS 출처 수집 실패 시 다른 출처는 계속 수집한다")
    void RSS_출처_실패_시_다른_출처는_계속_수집한다() {
      // given
      RssArticleDto chosunItem = new RssArticleDto(
          ArticleSource.CHOSUN, "https://chosun.com/1", "조선 기사", Instant.now(), "요약");
      given(naverNewsClient.fetchNews()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willThrow(new RuntimeException("RSS 오류"));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of(chosunItem));
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());

      // when & then
      assertThatNoException().isThrownBy(() -> newsCollectService.collect());
      verify(articleUpsertService).upsertAll(eq(ArticleSource.CHOSUN), anyList());
    }

    @Test
    @DisplayName("출처별 수집 건수를 집계한다")
    void 출처별_수집_건수를_집계한다() {
      // given
      NaverNewsItem item = new NaverNewsItem(
          "제목", "https://example.com/1", "https://example.com/1",
          "요약", "Mon, 29 May 2026 00:00:00 +0900");
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then — Naver 1건 수집이 출처별 건수로 집계된다
      verify(newsCollectMetrics).countCollected(ArticleSource.NAVER, 1);
    }

    @Test
    @DisplayName("pubDate가 null인 Naver 기사는 candidates에 포함하지 않고 건너뛴다")
    void pubDate가_null인_Naver_기사는_건너뛴다() {
      // given — pubDate null → parseNaverDate() → Optional.empty() → skip
      NaverNewsItem item = new NaverNewsItem("제목", "https://example.com/1", "https://example.com/1",
          "요약", null);
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then — candidates가 비어있으므로 upsertAll에는 빈 목록이 전달됨
      verify(articleUpsertService).upsertAll(eq(ArticleSource.NAVER),
          argThat(List::isEmpty));
    }

    @Test
    @DisplayName("originallink가 없는 Naver 기사는 link를 sourceUrl로 사용한다")
    void originallink가_없는_기사는_link를_sourceUrl로_사용한다() {
      // given — originallink null → link 사용
      NaverNewsItem item = new NaverNewsItem("제목", null, "https://example.com/1", "요약",
          "Mon, 29 May 2026 00:00:00 +0900");
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then
      verify(articleUpsertService).upsertAll(
          eq(ArticleSource.NAVER),
          argThat(list -> !list.isEmpty() && list.get(0).sourceUrl().equals("https://example.com/1")));
    }
  }
}
