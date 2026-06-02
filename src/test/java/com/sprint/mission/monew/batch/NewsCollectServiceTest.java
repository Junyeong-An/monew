package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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

  @Nested
  @DisplayName("뉴스 수집")
  class Collect {

    @Test
    @DisplayName("Naver 기사를 수집하면 ArticleUpsertService에 위임한다")
    void Naver_기사를_수집하면_ArticleUpsertService에_위임한다() {
      // given
      NaverNewsItem item = new NaverNewsItem(
          "제목", "https://example.com/1", "https://example.com/1",
          "요약", "Mon, 29 May 2026 00:00:00 +0900");
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then
      verify(articleUpsertService).upsert(
          eq(ArticleSource.NAVER), eq("https://example.com/1"), any(), any(), any());
    }

    @Test
    @DisplayName("RSS 기사를 수집하면 ArticleUpsertService에 위임한다")
    void RSS_기사를_수집하면_ArticleUpsertService에_위임한다() {
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
      verify(articleUpsertService).upsert(
          eq(ArticleSource.HANKYUNG), eq("https://hankyung.com/1"), any(), any(), any());
    }

    @Test
    @DisplayName("한 출처 수집 실패 시 다른 출처는 계속 수집한다")
    void 한_출처_실패_시_다른_출처는_계속_수집한다() {
      // given
      RssArticleDto rssItem = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");
      given(naverNewsClient.fetchNews()).willThrow(new RuntimeException("Naver API 오류"));
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(rssItem));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());

      // when & then — 예외 없이 완료, HANKYUNG은 upsert 호출됨
      assertThatNoException().isThrownBy(() -> newsCollectService.collect());
      verify(articleUpsertService).upsert(
          eq(ArticleSource.HANKYUNG), eq("https://hankyung.com/1"), any(), any(), any());
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
      verify(articleUpsertService).upsert(
          eq(ArticleSource.CHOSUN), eq("https://chosun.com/1"), any(), any(), any());
    }

    @Test
    @DisplayName("sourceUrl이 null인 Naver 기사는 upsert를 호출하지 않는다")
    void sourceUrl이_null인_기사는_upsert를_호출하지_않는다() {
      // given — originallink, link 모두 null
      NaverNewsItem item = new NaverNewsItem("제목", null, null, "요약",
          "Mon, 29 May 2026 00:00:00 +0900");
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then — sourceUrl=null이어도 upsert는 호출되나 내부에서 skip
      verify(articleUpsertService).upsert(eq(ArticleSource.NAVER), eq(null), any(), any(), any());
    }
  }
}
