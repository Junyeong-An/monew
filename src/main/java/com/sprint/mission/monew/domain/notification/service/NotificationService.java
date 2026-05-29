package com.sprint.mission.monew.domain.notification.service;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.notification.dto.NotificationQueryCondition;
import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import com.sprint.mission.monew.domain.notification.entity.Notification;
import com.sprint.mission.monew.domain.notification.exception.NotificationNotFoundException;
import com.sprint.mission.monew.domain.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository notificationRepository;

  public CursorPageResponse<NotificationResponse> findUnconfirmed(UUID userId,
      NotificationQueryCondition condition) {
    return notificationRepository.findUnconfirmed(userId, condition);
  }

  @Transactional
  public void confirmAll(UUID userId) {
    notificationRepository.confirmAllByUserId(userId, Instant.now());
  }

  @Transactional
  public void deleteExpiredNotifications() {
    Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
    int deleted = notificationRepository.deleteConfirmedBefore(cutoff);
    log.info("만료 알림 삭제 완료: {}건", deleted);
  }

  @Transactional
  public void confirm(UUID notificationId, UUID userId) {
    Notification notification = notificationRepository.findByIdAndUserIdAndConfirmedAtIsNull(notificationId, userId)
        .orElseThrow(() -> NotificationNotFoundException.withId(notificationId));
    notification.confirm();
  }
} 