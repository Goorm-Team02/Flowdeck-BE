package com.flowdeck.backend.user.repository;

import com.flowdeck.backend.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByPublicId(String publicId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
