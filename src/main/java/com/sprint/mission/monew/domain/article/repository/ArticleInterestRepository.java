package com.sprint.mission.monew.domain.article.repository;

import com.sprint.mission.monew.domain.article.entity.ArticleInterest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {}
