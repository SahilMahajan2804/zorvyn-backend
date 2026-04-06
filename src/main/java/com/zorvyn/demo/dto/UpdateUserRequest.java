package com.zorvyn.demo.dto;

import com.zorvyn.demo.entity.Role;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateUserRequest {
    private Role role;
    private Boolean active;
}
