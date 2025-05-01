package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.followers WHERE u.email = :email")
    Optional<User> findByEmailWithFollowers(@Param("email") String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}