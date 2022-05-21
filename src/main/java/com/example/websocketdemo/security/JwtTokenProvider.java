package com.example.websocketdemo.security;

import com.example.websocketdemo.exception.CustomException;
import com.example.websocketdemo.exception.InvalidFieldsException;
import com.example.websocketdemo.model.ChatMode;
import com.example.websocketdemo.model.Role;
import com.example.websocketdemo.model.Target;
import com.example.websocketdemo.utility.Authorization;
import com.example.websocketdemo.utility.PairValue;
import com.example.websocketdemo.utility.Utility;
import com.example.websocketdemo.utility.ZipUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.example.websocketdemo.WebsocketDemoApplication.classRepository;
import static com.example.websocketdemo.WebsocketDemoApplication.userRepository;
import static com.example.websocketdemo.utility.Statics.SOCKET_TOKEN_EXPIRATION_MSEC;
import static com.example.websocketdemo.utility.Statics.TOKEN_REUSABLE;
import static com.mongodb.client.model.Filters.*;

@Component
public class JwtTokenProvider {

    /**
     * THIS IS NOT A SECURE PRACTICE! For simplicity, we are storing a static key here. Ideally, in a
     * microservices environment, this key would be kept on a config-server.
     */
    final static private String secretKey = "{MIP0kK^PGU;l/{";
    final static private String secretSocketKey = "eFe;ek+;6{B95cU=";

    @Value("${security.jwt.token.expire-length:3600000}")

    private static MyUserDetails myUserDetails = new MyUserDetails();
    HashMap<ObjectId, PairValue> cachedTokens = new HashMap<>();
    //token, expiration_time
    HashMap<String, Long> validatedTokens = new HashMap<>();

    private String getSharedKeyBytes(boolean isForSocket) {
        return Base64.getEncoder().encodeToString(isForSocket ? secretSocketKey.getBytes() : secretKey.getBytes());
    }

    public String createToken(Document user)
            throws InvalidFieldsException {

        ObjectId userId = user.getObjectId("_id");
        String oldToken = null;

        if (cachedTokens.containsKey(userId)) {

            if (System.currentTimeMillis() < (long) cachedTokens.get(userId).getKey())
                return (String) cachedTokens.get(userId).getValue();

            if (System.currentTimeMillis() - (long) cachedTokens.get(userId).getKey() < 2 * SOCKET_TOKEN_EXPIRATION_MSEC)
                oldToken = (String) cachedTokens.get(userId).getValue();
        }

        Claims claims = Jwts.claims().setSubject(user.getString("username"));
        claims.put("user_id", userId.toString());
        int reuse = 0;

        List<Target> targets;
        boolean reFillTargets = true;

        if (oldToken != null) {

            try {
                Jwts.parser().setSigningKey(getSharedKeyBytes(true)).parseClaimsJws(oldToken);
                return oldToken;

            } catch (ExpiredJwtException e) {

                if (!e.getClaims().get("user_id").equals(userId.toString()))
                    throw new InvalidFieldsException("invalid old token 1");

                if (!e.getClaims().get("digest").equals(
                        e.getClaims().get("user_id") + "_" + e.getClaims().getSubject() + "_" +
                                e.getClaims().get("access")
                ))
                    throw new InvalidFieldsException("invalid old token 2");

                reuse = (int) e.getClaims().get("reuse");
                if (reuse >= TOKEN_REUSABLE)
                    reuse = 0;
                else {
                    reuse++;
                    claims.put("targets", e.getClaims().get("targets").toString());
                    reFillTargets = false;
                }
            }
        }


        if (reFillTargets) {

            targets = new ArrayList<>();

            ArrayList<Object> currentClassAndTeachers = Authorization.isTeacher(user.getString("access")) ?
                    getCurrentClassIds(userId) :
                    getCurrentClassAndTeacherIds(user);

            if (Authorization.isTeacher(user.getString("access"))) {

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

                        targets.add(new Target(ChatMode.PEER, studentId,
                                userDoc.getString("name_fa") + " " + userDoc.getString("last_name_fa"))
                        );

//                        break;
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
                    targets.add(new Target(ChatMode.PEER, theClass.getObjectId("teacher_id"), (String) p.getValue()));
                }

            }

            System.out.println(Target.toJSONArray(targets).toString().getBytes().length);
            String s = ZipUtils.compress(Target.toJSONArray(targets).toString());
            System.out.println(s.getBytes().length);

            claims.put("targets", s);
        }

        claims.put("reuse", reuse);
        claims.put("name", user.getString("name_fa") + " " + user.getString("last_name_fa"));
        claims.put("access", user.getString("access"));
        claims.put("digest", user.getObjectId("_id").toString() + "_" +
                user.getString("username") + "_" + user.getString("access"));

        Date now = new Date();
        long expireTime = now.getTime() + SOCKET_TOKEN_EXPIRATION_MSEC;

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(expireTime))
                .signWith(SignatureAlgorithm.HS256, getSharedKeyBytes(true))
                .compact();

        cachedTokens.put(user.getObjectId("_id"), new PairValue(expireTime, token));
        validatedTokens.put(token, expireTime);

        return token;
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

        output.put("_id", new ObjectId(claims.get("user_id").toString()));
        output.put("username", claims.getSubject());
        output.put("targets", ZipUtils.extract(claims.get("targets").toString()));
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
                    teacher.getString("name_fa") + " " + teacher.getString("last_name_fa")
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
