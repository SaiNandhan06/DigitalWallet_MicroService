package com.wallet.user.dto;

public class UpdateUserRequest {
    private String name;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
