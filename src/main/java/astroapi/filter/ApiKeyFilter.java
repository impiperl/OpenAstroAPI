package astroapi.filter;

import astroapi.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Filter to validate API key for secured endpoints.
 *
 * <p>This filter intercepts all incoming HTTP requests and checks the presence
 * and validity of an API key in the request headers. If the API key is missing
 * or invalid, the filter will respond with an HTTP 401 (Unauthorized) status.</p>
 *
 * <p>Excluded paths (e.g., public endpoints) can be configured in the
 * {@code shouldNotFilter} method.</p>
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Autowired
    private AppProperties apiKeyProperties;

    /**
     * Filters incoming requests to validate the API key.
     *
     * @param request     The HTTP request.
     * @param response    The HTTP response.
     * @param filterChain The filter chain to proceed with further processing.
     * @throws ServletException If an error occurs during filtering.
     * @throws IOException      If an I/O error occurs during filtering.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-KEY");

        if (!StringUtils.hasText(apiKey) || !apiKey.equals(apiKeyProperties.getSecurity().getSecretKey())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Specifies paths that should be excluded from the filter.
     *
     * @param request The HTTP request.
     * @return {@code true} if the filter should not apply to the given request.
     * @throws ServletException If an error occurs during filtering.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Exclude paths that start with /public/
        return path.startsWith("/public/");
    }
}
