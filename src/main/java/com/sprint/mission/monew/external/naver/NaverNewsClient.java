package com.sprint.mission.monew.external.naver;

import com.sprint.mission.monew.common.util.HtmlUtils;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.naver.dto.NaverNewsResponse;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsClient {

  private static final String NEWS_URL =
      "https://openapi.naver.com/v1/search/news.json?query={query}&display={display}&sort=date";

  private final RestClient restClient;

  @Value("${monew.naver.client-id:}")
  private String clientId;

  @Value("${monew.naver.client-secret:}")
  private String clientSecret;

  @Value("${monew.naver.query:뉴스}")
  private String query;

  @Value("${monew.naver.display:100}")
  private int display;

  public List<NaverNewsItem> fetchNews() {
    NaverNewsResponse response = restClient.get()
        .uri(NEWS_URL, query, display)
        .header("X-Naver-Client-Id", clientId)
        .header("X-Naver-Client-Secret", clientSecret)
        .retrieve()
        .body(NaverNewsResponse.class);

    if (response == null || response.items() == null) {
      return List.of();
    }
    return response.items();
  }

  public static Instant parseNaverDate(String pubDate) {
    try {
      return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
    } catch (Exception e) {
      log.warn("Naver 기사 날짜 파싱 실패: pubDate={}", pubDate, e);
      return Instant.now();
    }
  }

  public static String stripHtml(String html) {
    return HtmlUtils.strip(html);
  }
}
