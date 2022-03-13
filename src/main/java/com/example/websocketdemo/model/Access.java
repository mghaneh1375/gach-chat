package com.example.websocketdemo.model;

public enum Access {

    LIBRARY, SUPERADMIN, TEACHER, ADMIN, STUDENT, ACCOUNTANT, EDUCATIONAL_MANAGER;

    public String getName() {
        return name().toLowerCase();
    }
}
