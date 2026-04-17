package com.salty.admin.auth.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class SendEmailCodeRequest {

    @NotBlank
    @Email
    private String email;

    private String scene = "register";

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }
}
