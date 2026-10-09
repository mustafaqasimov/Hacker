package az.hacktrain.auth;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.util.*;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
class AuthSecurity {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean JwtEncoder jwtEncoder(AuthProperties p) { return new NimbusJwtEncoder(new ImmutableSecret<>(Base64.getDecoder().decode(p.jwtSecret()))); }
    @Bean JwtDecoder jwtDecoder(AuthProperties p) {
        var decoder=NimbusJwtDecoder.withSecretKey(new SecretKeySpec(Base64.getDecoder().decode(p.jwtSecret()),"HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audience=jwt -> jwt.getAudience().contains(p.audience())
                ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(p.issuer()),audience));
        return decoder;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, AuthProperties p, UserRepository users, AuthRateLimit limiter, @org.springframework.beans.factory.annotation.Value("${hacktrain.auth.email-confirmation-required:false}") boolean confirmationRequired) throws Exception {
        http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(cors(p)))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health/**","/actuator/prometheus").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                    "/api/v1/auth/register","/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/logout",
                    "/api/v1/auth/verify-email","/api/v1/auth/resend-verification","/api/v1/auth/forgot-password","/api/v1/auth/reset-password").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwt -> {
                try {
                    var user=users.findById(UUID.fromString(jwt.getSubject())).orElseThrow(AuthFailure::invalid);
                    Number version=jwt.getClaim("ver");
                    if (user.blocked || (confirmationRequired && !user.emailVerified) || version==null || user.tokenVersion!=version.longValue()) throw AuthFailure.invalid();
                    return new JwtAuthenticationToken(jwt,List.of(new SimpleGrantedAuthority("ROLE_"+user.platformRole)));
                } catch (RuntimeException e) { throw new org.springframework.security.authentication.BadCredentialsException("Invalid account"); }
            })).authenticationEntryPoint((req,res,e) -> ProblemWriter.write(res,401,"invalid_token","Giriş tələb olunur.")))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,x) -> ProblemWriter.write(res,401,"unauthorized","Giriş tələb olunur."))
                .accessDeniedHandler((req,res,x) -> ProblemWriter.write(res,403,"forbidden","İcazə yoxdur.")))
            .addFilterBefore(limiter,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    private CorsConfigurationSource cors(AuthProperties p) {
        var config=new CorsConfiguration(); config.setAllowedOrigins(p.corsOrigins());
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type","Idempotency-Key"));
        config.setExposedHeaders(List.of("Retry-After")); config.setAllowCredentials(false);
        var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",config); return source;
    }
    @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<AuthRateLimit> disableContainerRegistration(AuthRateLimit filter) {
        var bean=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter); bean.setEnabled(false); return bean;
    }
}
