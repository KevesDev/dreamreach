package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.PlayerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerAccountRepository extends JpaRepository<PlayerAccount, UUID> {

    Optional<PlayerAccount> findByEmail(String email);

    /**
     * For existsBy:
     * Spring automatically writes the SQL to check if a name/email is taken,
     * which is good for the registration page.
     * @param email
     * @return
     */
    boolean existsByEmail(String email);
}