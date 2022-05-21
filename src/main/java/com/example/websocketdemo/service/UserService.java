package com.example.websocketdemo.service;

import com.example.websocketdemo.security.JwtTokenProvider;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

import static com.example.websocketdemo.WebsocketDemoApplication.userRepository;

@Service
public class UserService {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public HashMap<String, Object> getClaims(HttpServletRequest req) {
        return jwtTokenProvider.getClaims(jwtTokenProvider.resolveToken(req));
    }

    public HashMap<String, Object> getClaims(String token) {
        return jwtTokenProvider.getClaims(token);
    }

    public Document whoAmI(HttpServletRequest req) {
        return userRepository.findByUsername(jwtTokenProvider.getUsername(jwtTokenProvider.resolveToken(req), false));
    }

}
