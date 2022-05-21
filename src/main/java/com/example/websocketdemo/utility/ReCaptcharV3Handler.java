package com.example.websocketdemo.utility;

import com.example.websocketdemo.exception.InvalidFieldsException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class ReCaptcharV3Handler {

    private String secretKey = "6LdAHAAgAAAAACrlCuqSBFuP4CI_l3SBRbtOUlsB";
    private String serverAddress = "https://www.google.com/recaptcha/api/siteverify";

    public float verify(String recaptchaFormResponse) throws InvalidFieldsException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("secret",secretKey);
        map.add("response",recaptchaFormResponse);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map,headers);

        RestTemplate restTemplate = new RestTemplate();
        ReCaptchaResponse response = restTemplate.postForObject(
                serverAddress,request, ReCaptchaResponse.class);


//        System.out.println("Success: " + response.isSuccess());
//        System.out.println("Score: " + response.getScore());

//        if (response.getErrorCodes() != null){
//            System.out.println("Error codes: ");
//            for (String errorCode: response.getErrorCodes()){
//                System.out.println("\t" + errorCode);
//            }
//        }

        if (response == null || !response.isSuccess()){
            throw new InvalidFieldsException("Invalid ReCaptha. Please check site");
        }
        // return 0.4f;
        return response.getScore();
    }
}