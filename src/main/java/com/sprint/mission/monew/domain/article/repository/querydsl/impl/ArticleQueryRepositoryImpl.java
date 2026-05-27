package com.sprint.mission.monew.domain.article.repository.querydsl.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.monew.domain.article.dto.ArticleSearchRequest;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.QArticle;
import com.sprint.mission.monew.domain.article.entity.QArticleInterest;
import com.sprint.mission.monew.domain.article.repository.querydsl.ArticleQueryRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class ArticleQueryRepositoryImpl implements ArticleQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Article> findAll(ArticleSearchRequest request) {
    QArticle article = QArticle.article;

    JPAQuery<Article> query =
        queryFactory
            .selectFrom(article)
            .where(buildPredicate(article, request))
            .orderBy(buildOrderSpecifiers(article, request))
            .limit(request.limit() + 1L);

    applyInterestJoin(query, article, request);

    return query.fetch();
  }

  @Override
  public long count(ArticleSearchRequest request) {
    QArticle article = QArticle.article;

    JPAQuery<Long> query =
        queryFactory.select(article.count()).from(article).where(buildPredicate(article, request));

    applyInterestJoin(query, article, request);

    Long result = query.fetchOne();
    return result != null ? result : 0L;
  }

  private <T> void applyInterestJoin(
      JPAQuery<T> query, QArticle article, ArticleSearchRequest request) {
    if (request.interestId() != null) {
      QArticleInterest articleInterest = QArticleInterest.articleInterest;
      query
          .join(articleInterest)
          .on(
              articleInterest
                  .article
                  .id
                  .eq(article.id)
                  .and(articleInterest.interest.id.eq(request.interestId())));
    }
  }

  private BooleanBuilder buildPredicate(QArticle article, ArticleSearchRequest request) {
    BooleanBuilder builder = new BooleanBuilder();

    builder.and(article.deletedAt.isNull());

    if (StringUtils.hasText(request.keyword())) {
      builder.and(
          article
              .title
              .containsIgnoreCase(request.keyword())
              .or(article.summary.containsIgnoreCase(request.keyword())));
    }

    if (request.sourceIn() != null && !request.sourceIn().isEmpty()) {
      builder.and(article.source.in(request.sourceIn()));
    }

    if (request.publishDateFrom() != null) {
      builder.and(article.publishDate.goe(request.publishDateFrom()));
    }

    if (request.publishDateTo() != null) {
      builder.and(article.publishDate.loe(request.publishDateTo()));
    }

    if (request.cursor() != null && request.after() != null) {
      builder.and(buildCursorCondition(article, request));
    }

    return builder;
  }

  private BooleanExpression buildCursorCondition(QArticle article, ArticleSearchRequest request) {
    boolean isDesc = "DESC".equalsIgnoreCase(request.direction());
    Instant after = request.after();

    return switch (request.orderBy()) {
      case "publishDate" -> {
        Instant cursorInstant = Instant.parse(request.cursor());
        yield isDesc
            ? article
                .publishDate
                .lt(cursorInstant)
                .or(article.publishDate.eq(cursorInstant).and(article.createdAt.lt(after)))
            : article
                .publishDate
                .gt(cursorInstant)
                .or(article.publishDate.eq(cursorInstant).and(article.createdAt.gt(after)));
      }
      case "commentCount" -> {
        long cursorVal = Long.parseLong(request.cursor());
        yield isDesc
            ? article
                .commentCount
                .lt(cursorVal)
                .or(article.commentCount.eq(cursorVal).and(article.createdAt.lt(after)))
            : article
                .commentCount
                .gt(cursorVal)
                .or(article.commentCount.eq(cursorVal).and(article.createdAt.gt(after)));
      }
      case "viewCount" -> {
        long cursorVal = Long.parseLong(request.cursor());
        yield isDesc
            ? article
                .viewCount
                .lt(cursorVal)
                .or(article.viewCount.eq(cursorVal).and(article.createdAt.lt(after)))
            : article
                .viewCount
                .gt(cursorVal)
                .or(article.viewCount.eq(cursorVal).and(article.createdAt.gt(after)));
      }
      default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준: " + request.orderBy());
    };
  }

  private OrderSpecifier<?>[] buildOrderSpecifiers(QArticle article, ArticleSearchRequest request) {
    Order dir = "DESC".equalsIgnoreCase(request.direction()) ? Order.DESC : Order.ASC;

    OrderSpecifier<?> primary =
        switch (request.orderBy()) {
          case "publishDate" -> new OrderSpecifier<>(dir, article.publishDate);
          case "commentCount" -> new OrderSpecifier<>(dir, article.commentCount);
          case "viewCount" -> new OrderSpecifier<>(dir, article.viewCount);
          default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준: " + request.orderBy());
        };

    return new OrderSpecifier<?>[] {primary, new OrderSpecifier<>(dir, article.createdAt)};
  }
}
