package com.finance.dashboard.repository;

import com.finance.dashboard.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByActiveTrue();

    List<User> findByRole(User.Role role);

    @Query("SELECT u FROM User u WHERE u.active = true AND u.role = :role")
    List<User> findActiveByRole(User.Role role);
}
