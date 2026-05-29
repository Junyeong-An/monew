package com.sprint.mission.monew.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.comment.dto.response.CommentLikeResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import com.sprint.mission.monew.domain.comment.exception.CommentLikeAlreadyExistsException;
import com.sprint.mission.monew.domain.comment.exception.CommentLikeNotFoundException;
import com.sprint.mission.monew.domain.comment.exception.CommentNotFoundException;
import com.sprint.mission.monew.domain.comment.mapper.CommentLikeMapper;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentLikeServiceTest {

  @InjectMocks
  private CommentLikeService commentLikeService;

  @Mock
  private CommentLikeRepository commentLikeRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private CommentLikeMapper commentLikeMapper;

  private UUID articleId;
  private UUID userId;
  private UUID commentId;
  private UUID commentLikeId;
  private Article article;
  private User user;
  private Comment comment;

  @BeforeEach
  void setUp() {
    article = Article.create(
        ArticleSource.NAVER,
        "https://example.com/news/1",
        "테스트 기사 제목",
        Instant.parse("2024-01-01T00:00:00Z"),
        "기사 요약 내용"
    );
    user = User.create("Test@naver.com", "test", "12345678");
    comment = Comment.create(article, user, "댓글 내용");
    articleId = article.getId();
    userId = user.getId();
    commentId = comment.getId();
    commentLikeId = UUID.randomUUID();
  }

  @Nested
  @DisplayName("댓글 좋아요 등록하기")
  class Service_CommentLike_Create {

    @Test
    @DisplayName("댓글 좋아요 등록 실패 - 유저가 존재하지 않음")
    void 댓글좋아요_등록_실패_유저_없음() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
          () -> commentLikeService.create(commentId, userId)).isInstanceOf(
          UserNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 좋아요 등록 실패 - 댓글이 존재하지 않음")
    void 댓글좋아요_등록_실패_댓글_없음() {
      // given
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(commentRepository.findById(commentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
          () -> commentLikeService.create(commentId, userId)).isInstanceOf(
          CommentNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 좋아요 등록 실패 - 이미 좋아요가 등록되어 있음")
    void 댓글좋아요_등록_실패_좋아요_중복등록() {
      // given
      given(commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)).willReturn(true);

      assertThatThrownBy(
          () -> commentLikeService.create(commentId, userId)).isInstanceOf(
          CommentLikeAlreadyExistsException.class);
    }

    @Test
    @DisplayName("댓글 좋아요 등록 성공")
    void 댓글_좋아요_등록_성공() {
      // given
      // comment(commentId, userId), user는 BeforeEach에서 초기화

      // 여기서는 자신의 댓글에 좋아요를 누른거로 테스트
      CommentLikeResponse expectedResponse = new CommentLikeResponse(
          commentLikeId,
          userId,
          Instant.now(),
          commentId,
          articleId,
          userId,
          "test2",
          "댓글 내용",
          0,
          Instant.now()
      );

      given(commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)).willReturn(false);
      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));

      CommentLike savedCommentLike = CommentLike.create(user, comment);
      given(commentLikeRepository.saveAndFlush(any(CommentLike.class))).willReturn(
          savedCommentLike);

      doNothing().when(commentRepository).increaseLikeCount(commentId);
      given(commentLikeMapper.toResponse(any(CommentLike.class))).willReturn(expectedResponse);

      // when
      CommentLikeResponse response = commentLikeService.create(commentId, userId);

      // then
      assertThat(response).isNotNull();
      assertThat(response).isEqualTo(expectedResponse);
      verify(commentRepository).increaseLikeCount(commentId);
      verify(commentLikeRepository).saveAndFlush(any(CommentLike.class));
      verify(commentLikeMapper).toResponse(any(CommentLike.class));
    }
  }

  @Nested
  @DisplayName("댓글 좋아요 취소하기")
  class Service_CommentLike_Cancel {

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 좋아요가 존재하지 않음")
    void 댓글_좋아요_취소_실패_좋아요_없음() {
      // given
      // commentId, userId는 BeforeEach에서 초기화

      // when & then
      assertThatThrownBy(
          () -> commentLikeService.cancel(commentId, userId)).isInstanceOf(
          CommentLikeNotFoundException.class);
    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void 댓글_좋아요_취소_성공() {
      // given
      given(commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId)).willReturn(1);

      // when
      commentLikeService.cancel(commentId, userId);

      // then
      verify(commentLikeRepository).deleteByUserIdAndCommentId(userId, commentId);
      verify(commentRepository).decreaseLikeCount(commentId);
    }
  }

}
