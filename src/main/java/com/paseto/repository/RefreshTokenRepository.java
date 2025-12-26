package com.paseto.repository;

import com.paseto.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByTokenId(String tokenId);

    List<RefreshToken> findByUserId(Long userId);

    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    List<RefreshToken> findByUserIdAndExpiredTrueOrRevokedTrue(Long userId);

    void deleteByExpiresAtBefore(LocalDateTime date);

    boolean existsByToken(String token);

    boolean existsByTokenId(String tokenId);
}
