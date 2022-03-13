package com.example.websocketdemo.validator;

import org.bson.types.ObjectId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static com.example.websocketdemo.utility.Statics.MAX_OBJECT_ID_SIZE;
import static com.example.websocketdemo.utility.Statics.MIN_OBJECT_ID_SIZE;


public class ObjectIdValidator implements
        ConstraintValidator<ObjectIdConstraint, ObjectId> {


    @Override
    public void initialize(ObjectIdConstraint constraintAnnotation) {

    }

    @Override
    public boolean isValid(ObjectId o, ConstraintValidatorContext constraintValidatorContext) {
        return o.toString().length() >= MIN_OBJECT_ID_SIZE && o.toString().length() <= MAX_OBJECT_ID_SIZE;
    }

    public static boolean isValid(String o) {
        return o.length() >= MIN_OBJECT_ID_SIZE && o.length() <= MAX_OBJECT_ID_SIZE;
    }
}
