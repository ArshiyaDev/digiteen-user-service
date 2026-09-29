package com.digiteen.userservice.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    @Query("""
            select u from UserEntity u
            where lower(u.email) = lower(:identifier) or u.phone = :identifier
            """)
    Optional<UserEntity> findByIdentifier(@Param("identifier") String identifier);
}
