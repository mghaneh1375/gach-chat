package com.example.websocketdemo.security;

import com.example.websocketdemo.db.UserRepository;
import com.example.websocketdemo.exception.CustomException;
import com.example.websocketdemo.model.Target;
import com.example.websocketdemo.utility.Authorization;
import com.example.websocketdemo.utility.PairValue;
import io.jsonwebtoken.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.example.websocketdemo.WebsocketDemoApplication.userRepository;
import static com.example.websocketdemo.utility.Statics.*;

@Component
public class JwtTokenProvider {

    /**
     * THIS IS NOT A SECURE PRACTICE! For simplicity, we are storing a static key here. Ideally, in a
     * microservices environment, this key would be kept on a config-server.
     */
    final static private String secretKey = "{MIP0kK^PGU;l/{";
    final static private String secretSocketKey = "eFe;ek+;6{B95cU=";
    final static private String secretServerKey = "zv#x![vph,YLf8/&";

    @Value("${security.jwt.token.expire-length:3600000}")

    private static MyUserDetails myUserDetails = new MyUserDetails();

    private class Token {

        long expiredAt;
        String token;
        List<Target> targets;

        public Token() {
        }

        public Token(long expiredAt, String token, List<Target> targets) {
            this.expiredAt = expiredAt;
            this.token = token;
            this.targets = targets;
        }

        public long getExpiredAt() {
            return expiredAt;
        }

        public String getToken() {
            return token;
        }

        public List<Target> getTargets() {
            return targets;
        }

        public void setExpiredAt(long expiredAt) {
            this.expiredAt = expiredAt;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public void setTargets(List<Target> targets) {
            this.targets = targets;
        }
    }


    HashMap<ObjectId, Token> cachedTokens = new HashMap<>();

    //token, expiration_time
    HashMap<String, Long> validatedTokens = new HashMap<>();

    private String getSharedKeyBytes(boolean isForSocket) {
        return Base64.getEncoder().encodeToString(isForSocket ? secretSocketKey.getBytes() : secretKey.getBytes());
    }

    private String getStringRole(List<String> accesses) {

        boolean isAdmin = Authorization.isAdmin(accesses);
        boolean isAdvisor = Authorization.isAdvisor(accesses);

        return isAdmin ? "admin" : isAdvisor ? "advisor" : "student";

    }

    public PairValue createToken(Document user) {

        ObjectId userId = user.getObjectId("_id");
        Token oldToken = null;
        long curr = System.currentTimeMillis();

        if (cachedTokens.containsKey(userId)) {

            if (curr + SOCKET_TOKEN_CAUTION_TIME < cachedTokens.get(userId).getExpiredAt())
                return new PairValue(cachedTokens.get(userId).getToken(),
                        cachedTokens.get(userId).getExpiredAt() - curr
                );

            if (curr - cachedTokens.get(userId).getExpiredAt() < 2 * SOCKET_TOKEN_EXPIRATION_MSEC)
                oldToken = cachedTokens.get(userId);
        }

        String username = user.containsKey("phone") ?
                user.getString("phone") :
                user.getString("mail");

        Claims claims = Jwts.claims().setSubject(username);

        claims.put("user_id", userId.toString());
        int reuse = 0;

        boolean reFillTargets = true;

        if (oldToken != null) {

            Claims e = null;

            try {
                e = Jwts.parser().setSigningKey(getSharedKeyBytes(true)).parseClaimsJws(oldToken.getToken()).getBody();
            } catch (ExpiredJwtException ex) {
                e = ex.getClaims();
            } finally {
                if (e != null) {
                    reuse = (int) e.get("reuse");
                    if (reuse >= TOKEN_REUSABLE)
                        reuse = 0;
                    else {
                        reuse++;
                        reFillTargets = false;
                    }
                }
            }
        } else
            oldToken = new Token();

        if (reFillTargets) {
            oldToken.setTargets(fetchTargetsList(userId,
                    Authorization.isAdvisor(user.getList("accesses", String.class)),
                    user)
            );
        }

        List<String> accesses = user.getList("accesses", String.class);
        String myRole = getStringRole(accesses);

        claims.put("reuse", reuse);
        claims.put("name", user.getString("first_name") + " " + user.getString("last_name"));
        claims.put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"));
        claims.put("accesses", accesses);
        claims.put("digest", user.getObjectId("_id").toString() + "_" +
                username + "_" + myRole);

        Date now = new Date();
        long expireTime = now.getTime() + SOCKET_TOKEN_EXPIRATION_MSEC;

        oldToken.setToken(Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(expireTime))
                .signWith(SignatureAlgorithm.HS256, getSharedKeyBytes(true))
                .compact()
        );

        oldToken.setExpiredAt(expireTime);


        if (cachedTokens.containsKey(userId))
            cachedTokens.put(user.getObjectId("_id"), oldToken);

        validatedTokens.put(oldToken.getToken(), expireTime);
        return new PairValue(oldToken.getToken(), expireTime - curr);
    }

    private List<Target> fetchTargetsList(ObjectId userId,
                                          boolean isAdvisor,
                                          Document user) {

        List<Target> targets = new ArrayList<>();

        if (user == null)
            user = userRepository.findById(userId);

        if (isAdvisor) {

            List<Document> students = user.getList("students", Document.class);

            for (Document student : students) {

                ObjectId studentId = student.getObjectId("_id");
                Document userDoc = userRepository.findById(studentId);

                if (userDoc == null)
                    continue;

                targets.add(
                        new Target(
                                studentId, userDoc.getString("first_name") + " " + userDoc.getString("last_name"),
                                userDoc.getString("pic")
                        )
                );
            }
        } else {

            Document userDoc = userRepository.findById(user.getObjectId("advisor_id"));

            targets.add(new Target(user.getObjectId("advisor_id"),
                    userDoc.getString("first_name") + " " + userDoc.getString("last_name"),
                    userDoc.getString("pic"))
            );

        }

        return targets;
    }

    Authentication getAuthentication(String token, boolean isForSocket) {
        UserDetails userDetails = myUserDetails.loadUserByUsername(getUsername(token, isForSocket));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUsername(String token, boolean isForSocket) {
        return Jwts.parser().setSigningKey(getSharedKeyBytes(isForSocket)).parseClaimsJws(token).getBody().getSubject();
    }

    public HashMap<String, Object> getClaims(String token) {

        HashMap<String, Object> output = new HashMap<>();

        Claims claims = Jwts.parser().setSigningKey(getSharedKeyBytes(true)).parseClaimsJws(token).getBody();

        ObjectId userId = new ObjectId(claims.get("user_id").toString());

        output.put("_id", userId);
        output.put("name", claims.get("name"));
        output.put("pic", claims.get("pic"));
        output.put("username", claims.getSubject());

        if (cachedTokens.containsKey(userId))
            output.put("targets", cachedTokens.get(userId).getTargets());
        else {

            List<Target> targets = fetchTargetsList(userId,
                    Authorization.isAdvisor((List<String>) claims.get("accesses")),
                    null
            );

            cachedTokens.put(userId, new Token(
                    claims.getExpiration().getTime(),
                    token,
                    targets
            ));

            output.put("targets", targets);
        }

        output.put("accesses", claims.get("accesses"));
        return output;
    }

    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token, boolean isForSocket) {

        if (validatedTokens.containsKey(token))
            return validatedTokens.get(token) >= System.currentTimeMillis();

        try {
            Jws<Claims> cliams = Jwts.parser().setSigningKey(getSharedKeyBytes(isForSocket)).parseClaimsJws(token);

            if (isForSocket) {

                List<String> accesses = (List<String>) cliams.getBody().get("accesses");
                String myRole = getStringRole(accesses);

                if (!cliams.getBody().get("digest").equals(
                        cliams.getBody().get("user_id") + "_" + cliams.getBody().getSubject() + "_" +
                                myRole
                ))
                    return false;
            }

            validatedTokens.put(token, cliams.getBody().getExpiration().getTime());
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("expire " + e.getMessage());
            throw new CustomException("Expired or invalid JWT token", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    boolean validateAuthToken(String token) {
        return validateToken(token, false);
    }

    boolean validateSocketToken(String token) {
        return validateToken(token, true);
    }

    HashMap<String, String> validateServerToken(String token) {

        try {
            Jws<Claims> cliams = Jwts.parser().setSigningKey(
                    Base64.getEncoder().encodeToString(secretServerKey.getBytes())
            ).parseClaimsJws(token);

            HashMap<String, String> out = new HashMap<>();

            for (String str : cliams.getBody().keySet())
                out.put(str, cliams.getBody().get(str).toString());

            return out;

        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("expire " + e.getMessage());
            throw new CustomException("Expired or invalid JWT token", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
