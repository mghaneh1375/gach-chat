package com.example.websocketdemo.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        final HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        response.setHeader("Access-Control-Allow-Origin",
                (request.getHeader("Origin") != null) ?
                    request.getHeader("Origin") : request.getHeader("Referer"));

	response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Cache-Control, Content-Type, Origin, X-Requested-With, Accept");
        response.setHeader("Access-Control-Max-Age", "3600");

//        //SEND OK or validate
        if ("OPTIONS".equalsIgnoreCase(((HttpServletRequest) req).getMethod())) {

//            if(((HttpServletRequest) req).getHeader("Origin") == null ||
//                !((HttpServletRequest) req).getHeader("Origin").equals("https://okft.org"))
//            {
//                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//                return;
//            }

            response.setStatus(HttpServletResponse.SC_OK);
        } else {
//            if(((HttpServletRequest) req).getHeader("Referer") == null ||
//                !((HttpServletRequest) req).getHeader("Referer").contains("https://okft.org"))
//            {
//                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//                return;
//            }
            chain.doFilter(req, res);
        }
    }

    @Override
    public void destroy() {
        //
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
    }
}
