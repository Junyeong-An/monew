package com.sprint.mission.monew.batch;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleBackupSchedulerTest {

  @InjectMocks ArticleBackupScheduler articleBackupScheduler;
  @Mock ArticleBackupService articleBackupService;

  @Nested
  @DisplayName("기사 S3 백업 스케줄러")
  class BackupSchedule {

    @Test
    @DisplayName("backup 호출 시 ArticleBackupService에 위임한다")
    void backup_호출_시_서비스에_위임한다() {
      // when
      articleBackupScheduler.backup();

      // then
      verify(articleBackupService).backup();
    }
  }
}
