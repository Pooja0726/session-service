package com.liveinterviewer.sessionservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetupSessionRequest {

    @NotBlank(message = "Role is required")
    private String role;

    private String company; // optional
}
