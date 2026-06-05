package com.sprint.mission.monew.domain.article.service;

import com.sprint.mission.monew.domain.article.dto.ArticleRestoreResultDto;
import java.time.Instant;
import java.util.List;

public interface ArticleRestoreService {

  List<ArticleRestoreResultDto> restore(Instant from, Instant to);
}
