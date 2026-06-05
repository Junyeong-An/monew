package com.sprint.mission.monew.domain.article.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.domain.article.dto.ArticleBackupEntry;
import com.sprint.mission.monew.domain.article.dto.ArticleRestoreResultDto;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.exception.ArticleRestoreFailedException;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleRestoreServiceImpl implements ArticleRestoreService {

  private static final DateTimeFormatter PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
  private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final ArticleRepository articleRepository;
  private final S3Client s3Client;
  private final ObjectMapper objectMapper;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  @Override
  @Transactional
  public List<ArticleRestoreResultDto> restore(Instant from, Instant to) {
    LocalDate fromDate = from.atZone(ZoneOffset.UTC).toLocalDate();
    LocalDate toDate = to.atZone(ZoneOffset.UTC).toLocalDate();

    List<ArticleRestoreResultDto> results = new ArrayList<>();
    for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
      restoreDate(date).ifPresent(results::add);
    }
    return results;
  }

  private Optional<ArticleRestoreResultDto> restoreDate(LocalDate date) {
    String s3Key = "articles/" + date.format(PATH_FORMATTER)
        + "/articles-" + date.format(FILE_FORMATTER) + ".json.gz";

    List<ArticleBackupEntry> entries;
    try {
      byte[] compressed = s3Client.getObject(
          GetObjectRequest.builder().bucket(bucket).key(s3Key).build()).readAllBytes();
      entries = objectMapper.readValue(
          gunzip(compressed),
          objectMapper.getTypeFactory().constructCollectionType(List.class, ArticleBackupEntry.class));
    } catch (NoSuchKeyException e) {
      log.info("백업 파일 없음, skip: {}", s3Key);
      return Optional.empty();
    } catch (Exception e) {
      log.error("백업 파일 읽기 실패: {}", s3Key, e);
      throw ArticleRestoreFailedException.withKey(s3Key, e);
    }

    if (entries.isEmpty()) {
      return Optional.empty();
    }

    List<String> sourceUrls = entries.stream().map(ArticleBackupEntry::sourceUrl).toList();
    Set<String> existingUrls = articleRepository.findBySourceUrlIn(sourceUrls)
        .stream().map(Article::getSourceUrl).collect(Collectors.toSet());

    List<Article> toRestore = entries.stream()
        .filter(e -> !existingUrls.contains(e.sourceUrl()))
        .map(e -> Article.create(e.source(), e.sourceUrl(), e.title(), e.publishDate(), e.summary()))
        .toList();

    if (toRestore.isEmpty()) {
      return Optional.empty();
    }

    List<Article> saved = articleRepository.saveAll(toRestore);
    List<java.util.UUID> restoredIds = saved.stream().map(Article::getId).toList();
    log.info("기사 복구 완료 | date={}, count={}", date, restoredIds.size());

    return Optional.of(new ArticleRestoreResultDto(
        date.atStartOfDay(ZoneOffset.UTC).toInstant(),
        restoredIds,
        restoredIds.size()));
  }

  private byte[] gunzip(byte[] compressed) throws IOException {
    try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
      return gis.readAllBytes();
    }
  }
}
