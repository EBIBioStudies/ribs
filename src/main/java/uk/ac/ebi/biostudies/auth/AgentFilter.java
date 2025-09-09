package uk.ac.ebi.biostudies.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AgentFilter implements Filter {

    @Value("${denied.agents:}")
    private String deniedAgentsRaw;

    private List<String> deniedAgents;

    @Override
    public void init(FilterConfig filterConfig) {
        deniedAgents = Arrays.stream(deniedAgentsRaw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            HttpServletResponse httpRes = (HttpServletResponse) response;

            String ua = httpReq.getHeader("User-Agent");
            if (StringUtils.hasText(ua)) {
                String uaLower = ua.toLowerCase(Locale.ROOT);

                boolean blocked = false;
                for (String denied : deniedAgents) {
                    if (uaLower.contains(denied)) {
                        blocked = true;
                        break;
                    }
                }

                if (blocked) {
                    httpRes.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    httpRes.getWriter().write("403 Forbidden – User-Agent blocked");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // no-op
    }
}

