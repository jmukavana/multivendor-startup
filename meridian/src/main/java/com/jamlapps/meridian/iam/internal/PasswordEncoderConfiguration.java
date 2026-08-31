package com.jamlapps.meridian.iam.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt for one-time codes, per ADR-008. Deliberately slow: a code is six digits, a million
 * possibilities, and the hash is what stands between that small space and an attacker who can
 * otherwise try all of them in the time a fast hash would take.
 */
@Configuration(proxyBeanMethods = false)
class PasswordEncoderConfiguration {

    @Bean
    PasswordEncoder oneTimeCodeEncoder() {
        return new BCryptPasswordEncoder();
    }
}
