package com.example.purchasebackend.repository;

import com.example.purchasebackend.domain.DeveloperUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeveloperUserRepository extends JpaRepository<DeveloperUser, String> {

    Optional<DeveloperUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
