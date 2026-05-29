package com.sprint.mission.monew.domain.comment.repository;

import com.sprint.mission.monew.domain.comment.entity.Comment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Comment c set c.likeCount = c.likeCount + 1
            where c.id = :commentId
      """)
  void increaseLikeCount(UUID commentId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Comment c set c.likeCount = c.likeCount - 1
            where c.id = :commentId and c.likeCount > 0
      """)
  void decreaseLikeCount(UUID commentId);

}
