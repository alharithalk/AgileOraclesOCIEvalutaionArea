package com.agileoracles.leave_portal_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/login.html", "/upload.html", "/files.html", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/error", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login.html")  // ← Use your custom login page
                        .defaultSuccessUrl("/upload.html", true)
                )
                .formLogin(form -> form.disable());  // ← DISABLE default form login
        return http.build();
    }
}