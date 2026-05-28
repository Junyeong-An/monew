package com.sprint.mission.monew.domain.article.mapper;

import com.sprint.mission.monew.domain.article.dto.ArticleViewResponse;
import com.sprint.mission.monew.domain.article.entity.ArticleView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleViewMapper {

  default ArticleViewResponse toResponse(ArticleView view) {
    throw new UnsupportedOperationException("not implemented");
  }
}
