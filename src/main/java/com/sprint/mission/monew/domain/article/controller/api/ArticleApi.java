package com.sprint.mission.monew.domain.article.controller.api;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleDto;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "뉴스 기사 관리", description = "뉴스 기사 API")
public interface ArticleApi {

  @Operation(summary = "뉴스 기사 목록 조회", description = "조건에 맞는 뉴스 기사 목록을 조회합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CursorPageResponse<ArticleDto>> search(
      @Parameter(description = "검색어(제목, 요약)") @RequestParam(required = false) String keyword,
      @Parameter(description = "관심사 ID") @RequestParam(required = false) UUID interestId,
      @Parameter(description = "출처(포함)") @RequestParam(required = false)
          List<ArticleSource> sourceIn,
      @Parameter(description = "날짜 시작(범위)") @RequestParam(required = false)
          Instant publishDateFrom,
      @Parameter(description = "날짜 끝(범위)") @RequestParam(required = false) Instant publishDateTo,
      @Parameter(description = "정렬 속성 이름", schema = @Schema(allowableValues = {"publishDate", "commentCount", "viewCount"}))
          @RequestParam
          String orderBy,
      @Parameter(description = "정렬 방향", schema = @Schema(allowableValues = {"ASC", "DESC"}))
          @RequestParam
          String direction,
      @Parameter(description = "커서 값") @RequestParam(required = false) String cursor,
      @Parameter(description = "보조 커서(createdAt) 값") @RequestParam(required = false) Instant after,
      @Parameter(description = "커서 페이지 크기", example = "50") @RequestParam int limit,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID")
          UUID requestUserId);
}
