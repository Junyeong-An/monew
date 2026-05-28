package com.sprint.mission.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QuerydslConfig.class)
class ArticleRepositoryTest {

  @Autowired ArticleRepository articleRepository;

  @BeforeEach
  void setUp() {
    articleRepository.deleteAll();
  }

  private Article saveArticle(ArticleSource source, String title) {
    return articleRepository.save(
        Article.create(source, "https://example.com/" + title, title, Instant.now(), "요약"));
  }

  private ArticleQueryCondition defaultCondition(int limit) {
    return new ArticleQueryCondition(
        null, null, null, null, null,
        ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
        null, null, limit);
  }

  @Nested
  @DisplayName("count")
  class Count {

    @Test
    @DisplayName("기사가 없으면 0을 반환한다")
    void 기사가_없으면_0을_반환한다() {
      // when
      long count = articleRepository.count(defaultCondition(10));

      // then
      assertThat(count).isZero();
    }

    @Test
    @DisplayName("저장된 기사 수만큼 반환한다")
    void 저장된_기사_수만큼_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");

      // when
      long count = articleRepository.count(defaultCondition(10));

      // then
      assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("소프트딜리트된 기사는 count에서 제외된다")
    void 소프트딜리트된_기사는_count에서_제외된다() {
      // given
      Article article = saveArticle(ArticleSource.NAVER, "삭제될기사");
      article.softDelete();
      articleRepository.save(article);
      saveArticle(ArticleSource.HANKYUNG, "정상기사");

      // when
      long count = articleRepository.count(defaultCondition(10));

      // then
      assertThat(count).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("findAll")
  class FindAll {

    @Test
    @DisplayName("cursor가 없으면 저장된 모든 기사를 반환한다")
    void cursor가_없으면_저장된_모든_기사를_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");

      // when
      List<Article> result = articleRepository.findAll(defaultCondition(10));

      // then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("소프트딜리트된 기사는 조회 결과에서 제외된다")
    void 소프트딜리트된_기사는_조회_결과에서_제외된다() {
      // given
      Article deleted = saveArticle(ArticleSource.NAVER, "삭제될기사");
      deleted.softDelete();
      articleRepository.save(deleted);
      saveArticle(ArticleSource.HANKYUNG, "정상기사");

      // when
      List<Article> result = articleRepository.findAll(defaultCondition(10));

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("정상기사");
    }

    @Test
    @DisplayName("limit+1개를 조회해 hasNext 판별에 사용할 수 있다")
    void limit_1개를_조회해_hasNext_판별에_사용할_수_있다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");
      saveArticle(ArticleSource.CHOSUN, "기사3");

      // when — limit=2이면 내부적으로 limit+1=3개 조회
      List<Article> result = articleRepository.findAll(defaultCondition(2));

      // then
      assertThat(result).hasSize(3); // 서비스에서 limit 초과 여부 판단
    }

    @Test
    @DisplayName("keyword로 필터링하면 제목에 keyword가 포함된 기사만 반환한다")
    void keyword로_필터링하면_제목에_keyword가_포함된_기사만_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "인공지능 뉴스");
      saveArticle(ArticleSource.HANKYUNG, "경제 동향");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          "인공지능", null, null, null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          null, null, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("인공지능 뉴스");
    }

    @Test
    @DisplayName("sourceIn 필터링하면 해당 출처의 기사만 반환한다")
    void sourceIn_필터링하면_해당_출처의_기사만_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "네이버 기사");
      saveArticle(ArticleSource.HANKYUNG, "한경 기사");
      saveArticle(ArticleSource.CHOSUN, "조선 기사");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, List.of(ArticleSource.NAVER, ArticleSource.HANKYUNG), null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          null, null, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).extracting(Article::getSource)
          .containsExactlyInAnyOrder(ArticleSource.NAVER, ArticleSource.HANKYUNG);
    }
  }
}
