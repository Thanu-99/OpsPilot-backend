package com.opspilot.opspilotbackend.repository;

import com.opspilot.opspilotbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSubject(String googleSubject);

    boolean existsByEmail(String email);

    List<User> findByManagerIdAndActiveTrue(Long managerId);

    List<User> findByCompanyIdAndActiveTrue(Long companyId);

    List<User> findByCompanyIdOrderByFirstNameAscLastNameAsc(Long companyId);
}
