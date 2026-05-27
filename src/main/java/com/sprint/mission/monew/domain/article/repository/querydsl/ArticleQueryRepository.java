package com.sprint.mission.monew.domain.article.repository.querydsl;

import com.sprint.mission.monew.domain.article.dto.ArticleSearchRequest;
import com.sprint.mission.monew.domain.article.entity.Article;
import java.util.List;

public interface ArticleQueryRepository {

  List<Article> findAll(ArticleSearchRequest request);

  long count(ArticleSearchRequest request);
}
