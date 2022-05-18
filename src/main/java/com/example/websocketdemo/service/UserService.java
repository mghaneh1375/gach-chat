package com.example.websocketdemo.service;

import com.example.websocketdemo.exception.CustomException;
import com.example.websocketdemo.exception.NotActivateAccountException;
import com.example.websocketdemo.model.Role;
import com.example.websocketdemo.security.JwtTokenProvider;
import com.example.websocketdemo.utility.Cache;
import com.example.websocketdemo.utility.PairValue;
import com.example.websocketdemo.utility.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;

import static com.example.websocketdemo.WebsocketDemoApplication.userRepository;
import static com.example.websocketdemo.utility.Statics.DEV_MODE;
import static com.example.websocketdemo.utility.Statics.TOKEN_EXPIRATION;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

@Service
public class UserService {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static ArrayList<Cache> cachedToken = new ArrayList<>();

//    public String signIn(String username, String password) throws NotActivateAccountException {
//
//        try {
//
//            PairValue p = new PairValue(username, password);
//
//            for(int i = 0; i < cachedToken.size(); i++) {
//                if(cachedToken.get(i).equals(p)) {
//                    if(cachedToken.get(i).checkExpiration())
//                        return (String) cachedToken.get(i).getValue();
//
//                    cachedToken.remove(i);
//                    break;
//                }
//            }
//
//            Document user = userRepository.findByUnique(username, false);
//
//            if(DEV_MODE) {
//                if (user == null)
//                    throw new CustomException("Invalid username/password supplied", HttpStatus.UNPROCESSABLE_ENTITY);
//            }
//            else {
//                if (user == null || !passwordEncoder.matches(password, user.getString("password")))
//                    throw new CustomException("Invalid username/password supplied", HttpStatus.UNPROCESSABLE_ENTITY);
//            }
//
//            if(!user.getString("status").equals("active"))
//                throw new NotActivateAccountException("Inactive account");
//
//            username = user.getString("username");
//            String token = jwtTokenProvider.createToken(username, Role.ROLE_CLIENT);
//            cachedToken.add(new Cache(TOKEN_EXPIRATION, token, new PairValue(user.getString("mail"), password)));
//            return token;
//
//        } catch (AuthenticationException x) {
//            x.printStackTrace();
//            throw new CustomException("Invalid username/password supplied", HttpStatus.UNPROCESSABLE_ENTITY);
//        }
//    }

//    public void logout(String token) {
//        for(int i = 0; i < cachedToken.size(); i++) {
//            if(cachedToken.get(i).getValue().equals(token)) {
//                cachedToken.remove(i);
//                return;
//            }
//        }
//    }

    public HashMap<String, Object> getClaims(HttpServletRequest req) {
        return jwtTokenProvider.getClaims(jwtTokenProvider.resolveToken(req));
    }

    public HashMap<String, Object> getClaims(String token) {
        return jwtTokenProvider.getClaims(token);
    }

    public Document whoAmI(HttpServletRequest req) {
        return userRepository.findByUsername(jwtTokenProvider.getUsername(jwtTokenProvider.resolveToken(req)));
    }

    public Document whoAmI(String token) {
        return userRepository.findByUsername(jwtTokenProvider.getUsername(token));
    }

}
