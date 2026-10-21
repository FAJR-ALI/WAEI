package org.fadhel.waeiplatform.security;

import org.fadhel.waeiplatform.model.Account;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${security.jwt.expiration-minutes:480}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Account account) {
        Instant now = Instant.now();

        Long organizationId = account.getOrganization() != null ? account.getOrganization().getId() : null;
        String organizationType = account.getOrganization() != null ? account.getOrganization().getType().name() : null;

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("waei-platform")
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .subject(account.getEmail())
                .claim("accountId", account.getId())
                .claim("role", account.getRole())
                .claim("fullName", account.getFullName());

        if (organizationId != null) {
            claims.claim("orgId", organizationId);
        }
        if (organizationType != null) {
            claims.claim("orgType", organizationType);
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
