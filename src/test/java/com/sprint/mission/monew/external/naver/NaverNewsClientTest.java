package com.sprint.mission.monew.external.naver;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NaverNewsClientTest {

  @Nested
  @DisplayName("parseNaverDate")
  class ParseNaverDate {

    @Test
    @DisplayName("RFC 1123 형식 날짜 문자열을 Instant로 변환한다")
    void RFC_1123_날짜를_Instant로_변환한다() {
      // given — +0900이면 UTC는 -9h → 2026-05-28T15:00:00Z
      String pubDate = "Mon, 29 May 2026 00:00:00 +0900";

      // when
      Instant result = NaverNewsClient.parseNaverDate(pubDate);

      // then
      assertThat(result).isEqualTo(Instant.parse("2026-05-28T15:00:00Z"));
    }

    @Test
    @DisplayName("파싱 불가능한 날짜는 현재 시각을 반환한다")
    void 파싱_불가능한_날짜는_현재_시각을_반환한다() {
      // given
      Instant before = Instant.now();

      // when
      Instant result = NaverNewsClient.parseNaverDate("invalid-date");

      // then
      Instant after = Instant.now();
      assertThat(result).isBetween(before, after);
    }

    @Test
    @DisplayName("null 날짜는 현재 시각을 반환한다")
    void null_날짜는_현재_시각을_반환한다() {
      // given
      Instant before = Instant.now();

      // when
      Instant result = NaverNewsClient.parseNaverDate(null);

      // then
      Instant after = Instant.now();
      assertThat(result).isBetween(before, after);
    }
  }
}
