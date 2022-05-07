package com.example.websocketdemo.security;

import com.example.websocketdemo.exception.CustomException;
import com.example.websocketdemo.utility.PairValue;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import static com.example.websocketdemo.utility.Statics.TOKEN_EXPIRATION;


// We should use OncePerRequestFilter since we are doing a database call, there is no point in doing this more than once
public class JwtTokenFilter extends OncePerRequestFilter {

    public class ValidateToken {

        private String token;
        private long issue;
        private Authentication authentication;

        ValidateToken(String token, Authentication authentication) {
            this.token = token;
            this.issue = System.currentTimeMillis();
            this.authentication = authentication;
        }

        public boolean isValidateYet() {
            return System.currentTimeMillis() - issue <= TOKEN_EXPIRATION; // 1 week
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof String)
                return o.equals(token);
            return false;
        }
    }

    private JwtTokenProvider jwtTokenProvider;
    private static final ArrayList<ValidateToken> validateTokens = new ArrayList<>();
    private static final ArrayList<PairValue> blackListTokens = new ArrayList<>();

    public static void removeTokenFromCache(String token) {

        for(int i = 0; i < validateTokens.size(); i++) {
            if(validateTokens.get(i).token.equals(token)) {
                blackListTokens.add(new PairValue(token, TOKEN_EXPIRATION + validateTokens.get(i).issue));
                validateTokens.remove(i);
                return;
            }
        }
    }

    public JwtTokenFilter() {
        jwtTokenProvider = new JwtTokenProvider();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    public boolean isAuth(HttpServletRequest request) {

        String token = jwtTokenProvider.resolveToken(request);

        if(token != null) {

            for (PairValue blackListToken : blackListTokens) {
                if (blackListToken.getKey().equals(token))
                    return false;
            }

            for (ValidateToken v : validateTokens) {
                if(v.equals(token)) {

                    if(v.isValidateYet())
                        return true;
                    else
                        validateTokens.remove(v);

                    break;
                }
            }
        }

        try {
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                validateTokens.add(new ValidateToken(token, auth));
                return true;
            }
        } catch (CustomException ex) {
            SecurityContextHolder.clearContext();
            return false;
        }

        return false;
    }

    public Authentication isAuth(String token) {

        if(token != null) {

            for (PairValue blackListToken : blackListTokens) {
                if (blackListToken.getKey().equals(token))
                    return null;
            }

            for (ValidateToken v : validateTokens) {

                if(v.equals(token)) {

                    if(v.isValidateYet())
                        return v.authentication;
                    else
                        validateTokens.remove(v);

                    break;
                }
            }
        }

        try {
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                validateTokens.add(new ValidateToken(token, auth));
                return auth;
            }
        } catch (CustomException ex) {
            SecurityContextHolder.clearContext();
            return null;
        }

        return null;
    }
}
