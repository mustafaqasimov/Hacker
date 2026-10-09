package az.hacktrain.auth;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
@Profile("dev")
class DevDocumentationSecurity {
    @Bean @Order(1)
    SecurityFilterChain documentation(HttpSecurity http) throws Exception {
        return http.securityMatcher("/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html")
            .authorizeHttpRequests(a->a.anyRequest().permitAll())
            .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'")))
            .build();
    }
}
