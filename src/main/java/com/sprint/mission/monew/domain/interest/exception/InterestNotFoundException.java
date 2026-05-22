package com.sprint.mission.monew.domain.interest.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class InterestNotFoundException extends InterestException {

  private InterestNotFoundException(Map<String, Object> details) {
    super(ErrorCode.INTEREST_NOT_FOUND, details);
  }

  public static InterestNotFoundException withId(UUID interestId) {
    return new InterestNotFoundException(Map.of("interestId", interestId));
  }
}