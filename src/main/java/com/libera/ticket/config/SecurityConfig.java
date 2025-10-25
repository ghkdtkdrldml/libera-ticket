package com.libera.ticket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${admin.user:admin}")
    private String adminUser;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    /*
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/main", "/rsvp", "/invite",  "/app/**", "/api/**", "/cancel/**", "/poster.jpg", "/Libera_program.jpg", "/h2-console/**", "/img/**", "/legal/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/libera/ticket").hasRole("LIBERA")
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .headers(h -> h.frameOptions(fo -> fo.sameOrigin()));

        return http.build();
    }
*/

    // 1) /admin/**  → Basic Auth (ROLE_ADMIN)
    @Bean
    @Order(1)
    SecurityFilterChain adminBasicChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("ADMIN"))
                .httpBasic(Customizer.withDefaults());   // ✅ Basic
        return http.build();
    }

    // 2) /libera/**  → Form Login (ROLE_LIBERA)
    @Bean
    @Order(2)
    SecurityFilterChain liberaFormChain(HttpSecurity http) throws Exception {
        http
                // ✅ 기본 로그인 페이지(/login)도 이 체인이 처리하도록 포함
                .securityMatcher("/libera/**", "/login")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login").permitAll()      // 기본 로그인 페이지 접근 허용
                        .requestMatchers("/libera/ticket").hasRole("LIBERA")
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")                 // 커스텀 로그인 페이지
                        .loginProcessingUrl("/login")        // 기본 처리 URL (POST /login)
                        .defaultSuccessUrl("/libera/ticket", false) // ✅ SavedRequest 우선, 없으면 /libera/ticket
                        .failureUrl("/login?error")
                        .permitAll())
                .httpBasic(httpBasic -> httpBasic.disable());   // 이 체인에서는 Basic 비활성화
        return http.build();
    }

    // 3) 그 외 경로 공개
    @Bean
    @Order(3)
    SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/main", "/rsvp", "/invite",
                                "/app/**", "/api/**", "/cancel/**",
                                "/poster.jpg", "/Libera_program.jpg",
                                "/h2-console/**", "/img/**", "/legal/**")
                        .permitAll()
                        .anyRequest().permitAll())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());
        return http.build();
    }


    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.withUsername(adminUser)
                .password(encoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        // ✅ /libera/ticket 전용 계정 (libera/libera1)
        UserDetails liberaViewer = User.withUsername("libera")
                .password(encoder.encode("libera1"))
                .roles("LIBERA")
                .build();

        return new InMemoryUserDetailsManager(admin, liberaViewer);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
