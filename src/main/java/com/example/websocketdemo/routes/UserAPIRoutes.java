package com.example.websocketdemo.routes;

import com.example.websocketdemo.controller.Router;
import com.example.websocketdemo.exception.NotActivateAccountException;
import com.example.websocketdemo.exception.UnAuthException;
import com.example.websocketdemo.security.JwtTokenFilter;
import com.example.websocketdemo.service.UserService;
import com.example.websocketdemo.utility.Utility;
import com.example.websocketdemo.validator.JSONConstraint;
import com.example.websocketdemo.validator.StrongJSONConstraint;
import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;


import static com.example.websocketdemo.WebsocketDemoApplication.userRepository;
import static com.example.websocketdemo.utility.Statics.*;
import static com.mongodb.client.model.Filters.eq;

@RestController
@RequestMapping(path = "/api/user")
@Validated
public class UserAPIRoutes extends Router {

    @Autowired
    UserService userService;

    @PostMapping(value = "/isAuth")
    @ResponseBody
    public String isAuth(HttpServletRequest request)
            throws UnAuthException, NotActivateAccountException {

        Document user = getUserWithOutCheckCompleteness(request);

        return new JSONObject()
                .put("id", user.getObjectId("_id").toString())
                .put("name", user.getString("name_fa") + " " + user.getString("last_name_fa"))
                .put("status", "ok")
                .toString();
    }

    @PostMapping(value = "/signIn")
    @ResponseBody
    public String signIn(@RequestBody @JSONConstraint(params = {
            "username", "password"
    }) @NotBlank String jsonStr) {
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);

            String username = jsonObject.getString("username").toLowerCase();
            String token = userService.signIn(
                    Utility.convertPersianDigits(username),
                    Utility.convertPersianDigits(jsonObject.getString("password"))
            );

            Document user = userRepository.findByUnique(username, false);

            return new JSONObject()
                    .put("token", token)
                    .put("id", user.getObjectId("_id").toString())
                    .put("status", "ok")
                    .toString();

        } catch (NotActivateAccountException x) {
            return new JSONObject().put("status", "nok").put("msg", "not active account").toString();
        } catch (Exception x) {
            x.printStackTrace();
            return JSON_NOT_VALID_PARAMS;
        }
    }

    @PostMapping(value = "/logout")
    @ResponseBody
    public String logout(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {

        getUserWithOutCheckCompleteness(request);

        try {
            String token = request.getHeader("Authorization");
            userService.logout(token);
            JwtTokenFilter.removeTokenFromCache(token.replace("Bearer ", ""));
            return JSON_OK;
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }

        return JSON_NOT_VALID_TOKEN;
    }

}
