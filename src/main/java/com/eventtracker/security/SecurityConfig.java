package com.eventtracker.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

/**
 * Who may do what:
 * <ul>
 *   <li>Sign-in page, static files and the health/info endpoints are public (Docker and Jenkins checks).</li>
 *   <li>Any signed-in user (VIEWER or ADMIN) can read every page, export and GET endpoint.</li>
 *   <li>Only ADMIN can open the create/edit forms and send POST, PUT or DELETE requests.</li>
 * </ul>
 * Browsers sign in with a form (session cookie, optional "remember me"); scripts and Jenkins use
 * HTTP Basic. Unauthenticated API calls get 401 instead of a redirect to the sign-in page.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private static final RequestMatcher API = new AntPathRequestMatcher("/api/**");
    private static final Duration REMEMBER_ME_VALIDITY = Duration.ofDays(14);

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties properties,
                                            AppUserDetailsService users) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/favicon.svg", "/error",
                                "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/events/new", "/events/*/edit",
                                "/sponsors/new", "/sponsors/*/edit").hasRole(Roles.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/**").authenticated()
                        .requestMatchers(HttpMethod.HEAD, "/**").authenticated()
                        .anyRequest().hasRole(Roles.ADMIN))
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard")
                        .failureHandler(loginFailureHandler())
                        .permitAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .rememberMe(remember -> remember
                        .key(rememberMeKey(properties))
                        .tokenValiditySeconds((int) REMEMBER_ME_VALIDITY.toSeconds())
                        .userDetailsService(users))
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID", "remember-me"))
                // The JSON API is called by scripts with HTTP Basic, which cannot carry a CSRF token.
                // Its write endpoints only accept JSON bodies or DELETE, which other sites cannot send cross-origin.
                .csrf(csrf -> csrf.ignoringRequestMatchers(API))
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), API)
                        // Everything else goes to the sign-in page, whatever the client's Accept header
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"),
                                AnyRequestMatcher.INSTANCE));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt by default; stored hashes carry an {id} prefix so the algorithm can change later
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    private static ExceptionMappingAuthenticationFailureHandler loginFailureHandler() {
        ExceptionMappingAuthenticationFailureHandler handler = new ExceptionMappingAuthenticationFailureHandler();
        handler.setExceptionMappings(Map.of(LockedException.class.getName(), "/login?locked"));
        handler.setDefaultFailureUrl("/login?error");
        return handler;
    }

    private static String rememberMeKey(SecurityProperties properties) {
        String key = properties.rememberMeKey();
        if (key != null && !key.isBlank()) {
            return key;
        }
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
