package com.example.websocketdemo.controller;

import com.example.websocketdemo.exception.NotAccessException;
import com.example.websocketdemo.exception.NotActivateAccountException;
import com.example.websocketdemo.exception.UnAuthException;
import com.example.websocketdemo.model.Target;
import com.example.websocketdemo.security.JwtTokenFilter;
import com.example.websocketdemo.service.UserService;
import com.example.websocketdemo.utility.Authorization;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

public class Router {

    @Autowired
    private UserService userService;

    private boolean isAdmin(HttpServletRequest request, Document u) throws NotActivateAccountException, NotAccessException {

        if (u != null) {

            if (!u.getString("status").equals("active")) {
                throw new NotActivateAccountException("Account not activated");
            }

            if (!Authorization.isAdmin(u.getString("access")))
                throw new NotAccessException("Access denied");

            return true;
        }

        return false;
    }

    protected HashMap<String, Object> getClaims(HttpServletRequest request)
            throws UnAuthException {

        boolean auth = new JwtTokenFilter().isAuth(request, true);
        if (auth)
            return userService.getClaims(request);

        System.out.println("heyyyyy5");
        throw new UnAuthException("Token is not valid");
    }

    HashMap<String, Object> getClaims(String token)
            throws UnAuthException {

        boolean isAuth = new JwtTokenFilter().isAuth(token, true);

        if (isAuth)
            return userService.getClaims(token);

        System.out.println("heyyyyy4");
        throw new UnAuthException("Token is not valid");
    }

    protected Document getUserWithOutCheckCompleteness(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {

        boolean auth = new JwtTokenFilter().isAuth(request, false);

        Document u;
        if (auth) {
            u = userService.whoAmI(request);
            if (u != null) {

                if (!u.getString("status").equals("active")) {
                    throw new NotActivateAccountException("Account not activated");
                }

                return u;
            }
        }

        System.out.println("heyyyyy3");
        throw new UnAuthException("Token is not valid");
    }

    void getUserWithToken(String token, String claimId, String targetId) {

        boolean isAuth = new JwtTokenFilter().isAuth(token, true);

        if (isAuth) {

            HashMap<String, Object> user = userService.getClaims(token);
            ObjectId userId = (ObjectId) user.get("_id");

            if (claimId != null && !userId.toString().equals(claimId)) {
                throw new AuthenticationCredentialsNotFoundException("Has no access");
            }

            if (targetId != null && Target.findInJSONArray(
                    new JSONArray(user.get("targets").toString()),
                    null, targetId
                    )
            )
                throw new AuthenticationCredentialsNotFoundException("Has no access");

            return;
        }

        throw new AuthenticationCredentialsNotFoundException("Token is not valid");
    }

    Authentication getSocketUserWithToken(String token) {

        Authentication auth = new JwtTokenFilter().isAuth(token);

        if (auth != null)
            return auth;

        System.out.println("heyyyyy2");
        throw new AuthenticationCredentialsNotFoundException("Token is not valid");
    }
}
