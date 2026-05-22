package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class UserNotFoundException extends UserException {

  private UserNotFoundException(Map<String, Object> details) {
    super(ErrorCode.USER_NOT_FOUND, details);
  }

  public static UserNotFoundException withId(UUID userId) {
    return new UserNotFoundException(Map.of("userId", userId));
  }

  public static UserNotFoundException withEmail(String email) {
    return new UserNotFoundException(Map.of("email", email));
  }
}