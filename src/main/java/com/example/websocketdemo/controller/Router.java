package com.example.websocketdemo.controller;

import com.example.websocketdemo.exception.NotAccessException;
import com.example.websocketdemo.exception.NotActivateAccountException;
import com.example.websocketdemo.exception.UnAuthException;
import com.example.websocketdemo.security.JwtTokenFilter;
import com.example.websocketdemo.service.UserService;
import com.example.websocketdemo.utility.Authorization;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public class Router {

    @Autowired
    private UserService userService;

    private boolean isAdmin(HttpServletRequest request, Document u) throws NotActivateAccountException, NotAccessException {

        if (u != null) {

            if(!u.getString("status").equals("active")) {
                JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                throw new NotActivateAccountException("Account not activated");
            }

            if(!Authorization.isAdmin(u.getString("access")))
                throw new NotAccessException("Access denied");

            return true;
        }

        return false;
    }

    protected Document getUserWithOutCheckCompleteness(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {

        boolean auth = new JwtTokenFilter().isAuth(request);

        Document u;
        if(auth) {
            u = userService.whoAmI(request);
            if (u != null) {

                if(!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(request.getHeader("Authorization").replace("Bearer ", ""));
                    throw new NotActivateAccountException("Account not activated");
                }

                return u;
            }
        }

        throw new UnAuthException("Token is not valid");
    }

    Document getUserWithToken(String token, String claimId) {

        Authentication auth = new JwtTokenFilter().isAuth(token);

        Document u;
        if(auth != null) {

            u = userService.whoAmI(token);
            if (u != null) {

                if(!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(token.replace("Bearer ", ""));
                    throw new AuthenticationCredentialsNotFoundException("Account is not active");
                }

                if(claimId != null && !u.getObjectId("_id").toString().equals(claimId))
                    throw new AuthenticationCredentialsNotFoundException("Has no access");

                return u;
            }
        }

        throw new AuthenticationCredentialsNotFoundException("Token is not valid");
    }

    Authentication getAuthUserWithToken(String token) {

        Authentication auth = new JwtTokenFilter().isAuth(token);

        Document u;
        if(auth != null) {

            u = userService.whoAmI(token);
            if (u != null) {

                if(!u.getString("status").equals("active")) {
                    JwtTokenFilter.removeTokenFromCache(token.replace("Bearer ", ""));
                    throw new AuthenticationCredentialsNotFoundException("Account is not active");
                }

                return auth;
            }
        }

        throw new AuthenticationCredentialsNotFoundException("Token is not valid");
    }
}
