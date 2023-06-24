package com.example.websocketdemo.routes;

import com.example.websocketdemo.controller.Router;
import com.example.websocketdemo.exception.NotActivateAccountException;
import com.example.websocketdemo.exception.UnAuthException;
import com.example.websocketdemo.service.UserService;
import com.example.websocketdemo.utility.PairValue;
import com.example.websocketdemo.utility.Utility;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

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

        return Utility.generateSuccessMsg(
                new PairValue("id", user.getObjectId("_id").toString()),
                new PairValue("name", user.getString("first_name") + " " + user.getString("last_name"))
        );

    }

}
