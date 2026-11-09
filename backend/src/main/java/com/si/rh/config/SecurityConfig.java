package com.si.rh.config;

// BY : Doneli

import com.si.rh.service.UtilisateurDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UtilisateurDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider provider,
            SecurityContextRepository contextRepository)
            throws Exception {

        http
                .authenticationProvider(provider)

                .cors(Customizer.withDefaults())

                .securityContext(context -> context
                        .securityContextRepository(contextRepository)
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository.withHttpOnlyFalse()
                        )
                        .csrfTokenRequestHandler(
                                new CsrfTokenRequestAttributeHandler()
                        )
                        .ignoringRequestMatchers("/api/auth/login")
                        .ignoringRequestMatchers("/api/auth/employe/login")
                        .ignoringRequestMatchers("/api/auth/logout")
                        .ignoringRequestMatchers("/api/auth/csrf")
                        .ignoringRequestMatchers("/api/public/**")
                        .ignoringRequestMatchers(request -> {
                            String path = request.getServletPath();
                            return path.startsWith("/api/conge/")
                                    && !path.matches(
                                            "/api/conge/demandes/\\d+/(validation|annulation)/?")
                                    && !path.matches(
                                            "/api/conge/remplacements/\\d+/decision/?");
                        })
                )

                // 401 au lieu de 403 quand l'utilisateur n'est pas connecté.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(
                                new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK)
                        )
                        .deleteCookies("JSESSIONID")
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/conge/demandes/en-attente")
                        .hasAnyRole("MANAGER", "RH")
                        .requestMatchers(HttpMethod.POST, "/api/conge/demandes/*/validation")
                        .hasAnyRole("MANAGER", "RH")
                        .requestMatchers(HttpMethod.GET, "/api/conge/demandes/*/validations")
                        .hasAnyRole("MANAGER", "RH")
                        .requestMatchers(HttpMethod.POST, "/api/conge/demandes/*/annulation")
                        .hasAnyRole("EMPLOYE", "RH")
                        .requestMatchers(HttpMethod.GET, "/api/conge/remplacements/moi")
                        .hasRole("EMPLOYE")
                        .requestMatchers(HttpMethod.POST, "/api/conge/remplacements/*/decision")
                        .hasRole("EMPLOYE")
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/employe/login",
                                "/api/client/**",
                                "/api/auth/csrf",
                                "/api/conge/**"
                        ).permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // La Direction a accès à toutes les zones.
                        .requestMatchers("/api/direction/**")
                        .hasRole("DIRECTION")

                        .requestMatchers(HttpMethod.GET, "/api/rh/contrats/a-embaucher").hasRole("RH")
                        .requestMatchers(HttpMethod.GET, "/api/rh/contrats").hasAnyRole("RH", "DIRECTION")
                        .requestMatchers(HttpMethod.GET, "/api/rh/contrats/*").hasAnyRole("RH", "DIRECTION")
                        .requestMatchers("/api/rh/contrats/**").hasRole("RH")
                        .requestMatchers("/api/rh/entretiens/**").hasRole("RH")
                        .requestMatchers(HttpMethod.POST, "/api/rh/employes/*/depart").hasRole("RH")
                        .requestMatchers("/api/rh/onboardings/taches/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/rh/regimes-travail/**")
                        .hasAnyRole("RH", "DIRECTION")
                        .requestMatchers(HttpMethod.POST, "/api/rh/regimes-travail/**").hasRole("RH")
                        .requestMatchers(HttpMethod.PUT, "/api/rh/regimes-travail/**").hasRole("RH")
                        .requestMatchers(HttpMethod.DELETE, "/api/rh/regimes-travail/**").hasRole("RH")
                        .requestMatchers(HttpMethod.GET, "/api/rh/horaires-travail/**")
                        .hasAnyRole("RH", "DIRECTION")
                        .requestMatchers(HttpMethod.POST, "/api/rh/horaires-travail/**").hasRole("RH")
                        .requestMatchers(HttpMethod.PUT, "/api/rh/horaires-travail/**").hasRole("RH")
                        .requestMatchers(HttpMethod.PATCH, "/api/rh/horaires-travail/**").hasRole("RH")
                        .requestMatchers(HttpMethod.DELETE, "/api/rh/horaires-travail/**").hasRole("RH")
                        .requestMatchers(HttpMethod.GET, "/api/rh/jours-feries/**")
                        .hasAnyRole("RH", "MANAGER", "DIRECTION")
                        .requestMatchers(HttpMethod.POST, "/api/rh/jours-feries/**").hasRole("RH")
                        .requestMatchers(HttpMethod.PUT, "/api/rh/jours-feries/**").hasRole("RH")
                        .requestMatchers(HttpMethod.DELETE, "/api/rh/jours-feries/**").hasRole("RH")
                        .requestMatchers(HttpMethod.GET, "/api/rh/plannings/options")
                        .hasRole("RH")
                        .requestMatchers(HttpMethod.GET, "/api/rh/plannings/departements")
                        .hasAnyRole("RH", "DIRECTION", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/rh/plannings/apercu")
                        .hasRole("RH")
                        .requestMatchers(HttpMethod.GET, "/api/rh/plannings/**")
                        .hasAnyRole("RH", "DIRECTION", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/rh/plannings/**").hasRole("RH")
                        .requestMatchers(HttpMethod.PUT, "/api/rh/plannings/**").hasRole("RH")
                        .requestMatchers(HttpMethod.PATCH, "/api/rh/plannings/**").hasRole("RH")
                        .requestMatchers(HttpMethod.DELETE, "/api/rh/plannings/**").hasRole("RH")
                        .requestMatchers(HttpMethod.GET, "/api/rh/variables-paie/**")
                        .hasAnyRole("RH", "DIRECTION", "MANAGER", "FINANCE")

                        .requestMatchers("/api/rh/besoins/**")
                        .hasAnyRole("RH", "FINANCE", "DIRECTION", "MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/rh/organigramme", "/api/rh/organigramme/**")
                        .hasAnyRole("RH", "DIRECTION", "MANAGER")
                        .requestMatchers("/api/rh/organigramme", "/api/rh/organigramme/**").denyAll()

                        .requestMatchers("/api/rh/**")
                        .hasAnyRole("RH", "DIRECTION")

                        .requestMatchers("/api/finance/**")
                        .hasAnyRole("FINANCE", "DIRECTION", "RH")

                        .requestMatchers("/api/manager/**")
                        .hasAnyRole("MANAGER", "DIRECTION", "RH")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}