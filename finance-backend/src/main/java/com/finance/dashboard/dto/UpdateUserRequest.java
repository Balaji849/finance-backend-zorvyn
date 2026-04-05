package com.finance.dashboard.dto;

import com.finance.dashboard.model.User;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    private User.Role role;

    private Boolean active;
}
