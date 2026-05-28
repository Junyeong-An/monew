package com.sprint.mission.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.entity.ArticleView;
import java.time.Instant;
import java.util.UUID;
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
@Import({JpaConfig.class, QuerydslConfig.class})
class ArticleViewRepositoryTest {

  @Autowired ArticleViewRepository articleViewRepository;
  @Autowired ArticleRepository articleRepository;

  @BeforeEach
  void setUp() {
    articleViewRepository.deleteAll();
    articleRepository.deleteAll();
  }

  private Article saveArticle() {
    return articleRepository.save(
        Article.create(ArticleSource.NAVER, "https://example.com/" + UUID.randomUUID(),
            "기사 제목", Instant.now(), "요약"));
  }

  @Nested
  @DisplayName("existsByArticleIdAndUserId")
  class ExistsByArticleIdAndUserId {

    @Test
    @DisplayName("조회 이력이 있으면 true를 반환한다")
    void 조회_이력이_있으면_true를_반환한다() {
      // given
      Article article = saveArticle();
      UUID userId = UUID.randomUUID();
      articleViewRepository.save(ArticleView.create(userId, article));

      // when & then
      assertThat(articleViewRepository.existsByArticleIdAndUserId(article.getId(), userId))
          .isTrue();
    }

    @Test
    @DisplayName("조회 이력이 없으면 false를 반환한다")
    void 조회_이력이_없으면_false를_반환한다() {
      // given
      Article article = saveArticle();
      UUID userId = UUID.randomUUID();

      // when & then
      assertThat(articleViewRepository.existsByArticleIdAndUserId(article.getId(), userId))
          .isFalse();
    }

    @Test
    @DisplayName("다른 사용자의 조회 이력은 false를 반환한다")
    void 다른_사용자의_조회_이력은_false를_반환한다() {
      // given
      Article article = saveArticle();
      UUID viewedUserId = UUID.randomUUID();
      UUID otherUserId = UUID.randomUUID();
      articleViewRepository.save(ArticleView.create(viewedUserId, article));

      // when & then
      assertThat(articleViewRepository.existsByArticleIdAndUserId(article.getId(), otherUserId))
          .isFalse();
    }
  }
}
