package com.example.websocketdemo.security;

import com.example.websocketdemo.exception.CustomException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


// We should use OncePerRequestFilter since we are doing a database call, there is no point in doing this more than once
public class JwtTokenFilter extends OncePerRequestFilter {

    private JwtTokenProvider jwtTokenProvider;

    public JwtTokenFilter() {
        jwtTokenProvider = new JwtTokenProvider();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    public boolean isAuth(HttpServletRequest request, boolean isForSocket) {
        return isAuth(jwtTokenProvider.resolveToken(request), isForSocket);
    }

    public boolean isAuth(String token, boolean isForSocket) {

        if(token != null) {
            try {
                if (
                    (isForSocket && jwtTokenProvider.validateSocketToken(token)) ||
                    (!isForSocket && jwtTokenProvider.validateAuthToken(token))
                ) {
                    Authentication auth = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    return true;
                }
            } catch (CustomException ex) {
                SecurityContextHolder.clearContext();
                return false;
            }
        }

        return false;
    }

    public Authentication isAuth(String token) {

        if(token != null) {
            try {
                if (jwtTokenProvider.validateSocketToken(token)) {
                    Authentication auth = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    return auth;
                }
            } catch (CustomException ex) {
                SecurityContextHolder.clearContext();
                return null;
            }
        }

        return null;
    }
}
