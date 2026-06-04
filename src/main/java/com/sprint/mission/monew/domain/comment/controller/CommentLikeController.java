package com.sprint.mission.monew.domain.comment.controller;

import com.sprint.mission.monew.domain.comment.controller.api.CommentLikeApi;
import com.sprint.mission.monew.domain.comment.dto.CommentLikeResponse;
import com.sprint.mission.monew.domain.comment.service.CommentLikeService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentLikeController implements CommentLikeApi {

  private final CommentLikeService commentLikeService;

  @Override
  @PostMapping("/{commentId}/comment-likes")
  public ResponseEntity<CommentLikeResponse> createCommentLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    CommentLikeResponse response = commentLikeService.create(commentId, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  @DeleteMapping("/{commentId}/comment-likes")
  public ResponseEntity<Void> cancelCommentLike(
      @PathVariable @Parameter(description = "댓글 ID") UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId) {
    commentLikeService.cancel(commentId, userId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

}
