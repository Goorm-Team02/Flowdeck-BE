package com.flowdeck.backend.user.service;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.dto.UserResponse;
import com.flowdeck.backend.user.dto.UserUpdateRequest;
import com.flowdeck.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public UserResponse getMyInfo(Long userId) {
    User user = getUser(userId);
    return UserResponse.from(user);
  }

  @Transactional
  public UserResponse updateMyInfo(Long userId, UserUpdateRequest request) {
    User user = getUser(userId);
    user.updateName(request.getName());

    return UserResponse.from(user);
  }

  private User getUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
