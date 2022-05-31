package com.example.websocketdemo.security;

import com.example.websocketdemo.db.UserRepository;
import com.example.websocketdemo.exception.CustomException;
import com.example.websocketdemo.model.ChatMode;
import com.example.websocketdemo.model.Target;
import com.example.websocketdemo.utility.Authorization;
import com.example.websocketdemo.utility.PairValue;
import com.example.websocketdemo.utility.Utility;
import com.mongodb.BasicDBObject;
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

import static com.example.websocketdemo.WebsocketDemoApplication.classRepository;
import static com.example.websocketdemo.WebsocketDemoApplication.userRepository;
import static com.example.websocketdemo.utility.Statics.*;
import static com.mongodb.client.model.Filters.*;

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

        Claims claims = Jwts.claims().setSubject(user.getString("username"));
        claims.put("user_id", userId.toString());
        int reuse = 0;

        boolean reFillTargets = true;

        if (oldToken != null) {

            Claims e = null;

            try {
                 e = Jwts.parser().setSigningKey(getSharedKeyBytes(true)).parseClaimsJws(oldToken.getToken()).getBody();
            } catch (ExpiredJwtException ex) {
                e = ex.getClaims();
            }
            finally {
                if(e != null) {
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
//            String s = ZipUtils.compress(Target.toJSONArray(targets).toString());
            oldToken.setTargets(fetchTargetsList(userId,
                    Authorization.isTeacher(user.getString("access")),
                    user)
            );
        }

        claims.put("reuse", reuse);
        claims.put("name", user.getString("name_fa") + " " + user.getString("last_name_fa"));
        claims.put("pic", STATICS_SERVER + UserRepository.FOLDER + "/" + user.getString("pic"));
        claims.put("access", user.getString("access"));
        claims.put("digest", user.getObjectId("_id").toString() + "_" +
                user.getString("username") + "_" + user.getString("access"));

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
                                          boolean isTeacher,
                                          Document user) {

        List<Target> targets = new ArrayList<>();
        ArrayList<Object> currentClassAndTeachers;

        if (isTeacher)
            currentClassAndTeachers = getCurrentClassIds(userId);
        else {

            if (user == null)
                user = userRepository.findById(userId);

            currentClassAndTeachers = getCurrentClassAndTeacherIds(user);
        }

        if (isTeacher) {

            for (Object o : currentClassAndTeachers) {

                ObjectId classId = (ObjectId) o;
                Document theClass = classRepository.findById(classId);

                if (theClass == null)
                    continue;

                targets.add(new Target(ChatMode.GROUP, classId, theClass.getString("name")));

                List<Document> students = theClass.getList("students", Document.class);
                for (Document student : students) {

                    ObjectId studentId = student.getObjectId("_id");
                    Document userDoc = userRepository.findById(studentId);

                    if (userDoc == null)
                        continue;

                    targets.add(new Target(studentId,
                            userDoc.getString("name_fa") + " " + userDoc.getString("last_name_fa"),
                            STATICS_SERVER + UserRepository.FOLDER + "/" + userDoc.getString("pic"),
                            classId)
                    );
                }
            }
        } else {

            for (Object o : currentClassAndTeachers) {

                PairValue p = (PairValue) o;
                ObjectId classId = (ObjectId) p.getKey();
                Document theClass = classRepository.findById(classId);

                if (theClass == null)
                    continue;

                targets.add(new Target(ChatMode.GROUP, classId, theClass.getString("name")));
                String[] splited = ((String) p.getValue()).split("&&&&");

                targets.add(new Target(
                                theClass.getObjectId("teacher_id"),
                                splited[0], splited[1], classId
                        )
                );
            }

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
//        output.put("targets", ZipUtils.extract(claims.get("targets").toString()));

        if (cachedTokens.containsKey(userId))
            output.put("targets", cachedTokens.get(userId).getTargets());
        else {

            List<Target> targets = fetchTargetsList(userId,
                    Authorization.isTeacher(claims.get("access").toString()),
                    null
            );

            cachedTokens.put(userId, new Token(
                    claims.getExpiration().getTime(),
                    token,
                    targets
            ));

            output.put("targets", targets);
        }

        output.put("access", claims.get("access"));
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

            if (isForSocket && !cliams.getBody().get("digest").equals(
                    cliams.getBody().get("user_id") + "_" + cliams.getBody().getSubject() + "_" +
                            cliams.getBody().get("access")
            ))
                return false;

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

            for(String str : cliams.getBody().keySet())
                out.put(str, cliams.getBody().get(str).toString());

            return out;

        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("expire " + e.getMessage());
            throw new CustomException("Expired or invalid JWT token", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private ArrayList<Object> getCurrentClassAndTeacherIds(Document user) {

        List<Document> passed = user.getList("passed", Document.class);
        ArrayList<Object> classAndTeacherIds = new ArrayList<>();
        ArrayList<ObjectId> courseIds = new ArrayList<>();
        int today = Utility.getToday();

        for (Document itr : passed) {

            if (!itr.containsKey("class_id") ||
                    itr.containsKey("success") ||
                    itr.containsKey("final_result") ||
                    courseIds.contains(itr.getObjectId("course_id"))
            )
                continue;

            Document theClass = classRepository.findById(itr.getObjectId("class_id"));

            if (theClass == null || !theClass.containsKey("teacher_id"))
                continue;

            if (theClass.getInteger("start") > today ||
                    theClass.getInteger("end") < today
            )
                continue;

            Document teacher = userRepository.findById(theClass.getObjectId("teacher_id"));
            if (teacher == null)
                continue;

            PairValue p = new PairValue(
                    theClass.getObjectId("_id"),
                    teacher.getString("name_fa") + " " + teacher.getString("last_name_fa") +
                            "&&&&" + STATICS_SERVER + UserRepository.FOLDER + "/" + teacher.getString("pic")
            );

            if (classAndTeacherIds.contains(p))
                continue;

            courseIds.add(itr.getObjectId("course_id"));
            classAndTeacherIds.add(p);
        }

        return classAndTeacherIds;
    }

    private ArrayList<Object> getCurrentClassIds(ObjectId teacherId) {

        int today = Utility.getToday();

        ArrayList<Document> docs = classRepository.find(and(
                eq("teacher_id", teacherId),
                lt("start", today),
                gt("end", today)
        ), new BasicDBObject("_id", 1));

        ArrayList<Object> classes = new ArrayList<>();
        for (Document doc : docs)
            classes.add(doc.getObjectId("_id"));

        return classes;
    }

}
