package pl.goeuropa.station.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.goeuropa.station.service.HmacVerificationService;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class HmacAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> EXACT_PUBLIC_PATHS = Set.of(
            "/verify-signature",
            "/swagger-ui.html"
    );
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/swagger-ui/",
            "/v3/api-docs"
    );

    private final HmacVerificationService hmacVerificationService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return EXACT_PUBLIC_PATHS.contains(path)
                || PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String signature = request.getHeader("x-signature");
        String timestamp = request.getHeader("x-timestamp");

        if (hmacVerificationService.verify(signature, timestamp)) {
            AbstractAuthenticationToken auth = new PreAuthenticatedAuthenticationToken(
                    "hmac-client",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
            );
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}