package com.chatterai.user.repository;

import com.chatterai.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);

    // DM 상대 검색 (활성 사용자만)
    List<User> findByNicknameContainingIgnoreCaseAndIsActiveTrue(String keyword, Pageable pageable);

    // 관리자 회원 목록 검색 (전체)
    Page<User> findByNicknameContainingIgnoreCase(String keyword, Pageable pageable);
}
