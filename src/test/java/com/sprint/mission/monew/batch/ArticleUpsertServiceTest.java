package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleInterestRepository;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
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
class ArticleUpsertServiceTest {

  @InjectMocks ArticleUpsertService articleUpsertService;
  @Mock ArticleRepository articleRepository;
  @Mock ArticleInterestRepository articleInterestRepository;
  @Mock InterestRepository interestRepository;
  @Mock NewsCollectMetrics newsCollectMetrics;

  private Interest 관심사_생성(String name, String... keywords) {
    return Interest.create(name, List.of(keywords));
  }

  @Nested
  @DisplayName("upsertAll")
  class UpsertAll {

    @Test
    @DisplayName("관심사 키워드와 매칭되는 신규 기사는 저장하고 ArticleInterest 매핑을 생성한다")
    void 매칭되는_신규_기사는_저장하고_매핑을_생성한다() {
      // given
      Interest interest = 관심사_생성("기술", "제목");
      ArticleCandidate candidate = new ArticleCandidate(
          "https://example.com/1", "제목 포함 기사", Instant.now(), "요약");
      Article saved = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "제목 포함 기사", Instant.now(), "요약");
      given(interestRepository.findAllWithKeywords()).willReturn(List.of(interest));
      given(articleRepository.findBySourceUrlIn(anyList())).willReturn(List.of());
      given(articleRepository.saveAll(anyList())).willReturn(List.of(saved));
      given(articleInterestRepository.saveAll(anyList())).willReturn(List.of());

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(candidate));

      // then
      verify(articleRepository).saveAll(anyList());
      verify(articleInterestRepository).saveAll(anyList());
      verify(newsCollectMetrics).countCreated(ArticleSource.NAVER);
    }

    @Test
    @DisplayName("관심사 키워드와 매칭되지 않는 신규 기사는 저장하지 않는다")
    void 매칭되지_않는_신규_기사는_저장하지_않는다() {
      // given
      Interest interest = 관심사_생성("기술", "AI");
      ArticleCandidate candidate = new ArticleCandidate(
          "https://example.com/1", "관련없는 기사 제목", Instant.now(), "관련없는 요약");
      given(interestRepository.findAllWithKeywords()).willReturn(List.of(interest));
      given(articleRepository.findBySourceUrlIn(anyList())).willReturn(List.of());

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(candidate));

      // then
      verify(articleRepository, never()).saveAll(any());
      verify(articleInterestRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("등록된 관심사가 없으면 기사를 저장하지 않는다")
    void 등록된_관심사가_없으면_기사를_저장하지_않는다() {
      // given
      ArticleCandidate candidate = new ArticleCandidate(
          "https://example.com/1", "제목", Instant.now(), "요약");
      given(interestRepository.findAllWithKeywords()).willReturn(List.of());

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(candidate));

      // then
      verify(articleRepository, never()).findBySourceUrlIn(any());
      verify(articleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("요약에 관심사 키워드가 포함되면 매칭으로 처리한다")
    void 요약에_키워드가_포함되면_매칭한다() {
      // given
      Interest interest = 관심사_생성("기술", "인공지능");
      ArticleCandidate candidate = new ArticleCandidate(
          "https://example.com/1", "일반 제목", Instant.now(), "인공지능 관련 요약");
      Article saved = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "일반 제목", Instant.now(), "인공지능 관련 요약");
      given(interestRepository.findAllWithKeywords()).willReturn(List.of(interest));
      given(articleRepository.findBySourceUrlIn(anyList())).willReturn(List.of());
      given(articleRepository.saveAll(anyList())).willReturn(List.of(saved));
      given(articleInterestRepository.saveAll(anyList())).willReturn(List.of());

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(candidate));

      // then
      verify(articleRepository).saveAll(anyList());
      verify(newsCollectMetrics).countCreated(ArticleSource.NAVER);
    }

    @Test
    @DisplayName("기존 기사는 관심사 매칭 여부와 무관하게 제목·요약을 업데이트한다")
    void 기존_기사는_업데이트하고_saveAll을_호출하지_않는다() {
      // given
      Article existing = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "원래 제목", Instant.now(), "원래 요약");
      ArticleCandidate candidate = new ArticleCandidate(
          "https://example.com/1", "수정된 제목", Instant.now(), "수정된 요약");
      given(interestRepository.findAllWithKeywords())
          .willReturn(List.of(관심사_생성("기술", "AI")));
      given(articleRepository.findBySourceUrlIn(anyList())).willReturn(List.of(existing));

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(candidate));

      // then
      verify(articleRepository, never()).saveAll(any());
      verify(newsCollectMetrics).countDuplicated(ArticleSource.NAVER);
      assertThat(existing.getTitle()).isEqualTo("수정된 제목");
      assertThat(existing.getSummary()).isEqualTo("수정된 요약");
    }

    @Test
    @DisplayName("소프트 삭제된 기사는 업데이트하지 않고 건너뛴다")
    void 소프트_삭제된_기사는_건너뛴다() {
      // given
      Article deleted = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "원래 제목", Instant.now(), "원래 요약");
      deleted.softDelete();
      ArticleCandidate candidate = new ArticleCandidate(
          "https://example.com/1", "새 제목", Instant.now(), "새 요약");
      given(interestRepository.findAllWithKeywords())
          .willReturn(List.of(관심사_생성("기술", "새 제목")));
      given(articleRepository.findBySourceUrlIn(anyList())).willReturn(List.of(deleted));

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(candidate));

      // then
      verify(articleRepository, never()).saveAll(any());
      verify(newsCollectMetrics, never()).countDuplicated(any());
      assertThat(deleted.getTitle()).isEqualTo("원래 제목");
    }

    @Test
    @DisplayName("candidates가 비어있으면 DB 조회를 하지 않는다")
    void candidates가_비어있으면_DB_조회를_하지_않는다() {
      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of());

      // then
      verify(interestRepository, never()).findAllWithKeywords();
      verify(articleRepository, never()).findBySourceUrlIn(any());
      verify(articleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("동일 sourceUrl 중복 candidates는 first-seen 하나만 저장한다")
    void 동일_sourceUrl_중복_candidates는_하나만_저장한다() {
      // given
      Interest interest = 관심사_생성("기술", "첫 번째");
      ArticleCandidate first = new ArticleCandidate(
          "https://example.com/1", "첫 번째 제목", Instant.now(), "첫 번째 요약");
      ArticleCandidate second = new ArticleCandidate(
          "https://example.com/1", "두 번째 제목", Instant.now(), "두 번째 요약");
      Article saved = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "첫 번째 제목", Instant.now(), "첫 번째 요약");
      given(interestRepository.findAllWithKeywords()).willReturn(List.of(interest));
      given(articleRepository.findBySourceUrlIn(anyList())).willReturn(List.of());
      given(articleRepository.saveAll(anyList())).willReturn(List.of(saved));
      given(articleInterestRepository.saveAll(anyList())).willReturn(List.of());

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(first, second));

      // then
      verify(articleRepository).saveAll(argThat((List<Article> list) ->
          list.size() == 1
              && list.get(0).getTitle().equals("첫 번째 제목")));
      verify(newsCollectMetrics).countCreated(ArticleSource.NAVER);
    }
  }
}
