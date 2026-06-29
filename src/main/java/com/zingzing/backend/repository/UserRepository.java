package com.zingzing.backend.repository;

import com.zingzing.backend.entity.User;
import com.zingzing.backend.entity.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByGoogleSubject(String googleSubject);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:active is null or u.active = :active)
              and (
                cast(:search as string) is null
                or lower(u.name) like concat('%', lower(cast(:search as string)), '%')
                or lower(u.email) like concat('%', lower(cast(:search as string)), '%')
                or lower(u.username) like concat('%', lower(cast(:search as string)), '%')
              )
            order by u.createdAt desc
            """)
    Page<User> searchForAdmin(@Param("search") String search,
                              @Param("role") UserRole role,
                              @Param("active") Boolean active,
                              Pageable pageable);

    long countByRole(UserRole role);

    long countByActiveTrue();

    long countByActiveFalse();

    @Modifying
    @Query("update User u set u.lastSeenAt = :seenAt where u.id = :userId")
    int updateLastSeenAt(@Param("userId") UUID userId, @Param("seenAt") Instant seenAt);
}
