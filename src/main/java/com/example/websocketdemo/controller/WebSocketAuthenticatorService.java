package com.example.websocketdemo.controller;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthenticatorService extends Router {

    Authentication getAuthenticatedOrFail(final String token) throws AuthenticationException {

        if (token == null || token.trim().isEmpty())
            throw new AuthenticationCredentialsNotFoundException("Token was null or empty.");

        return getSocketUserWithToken(token);
    }
}
