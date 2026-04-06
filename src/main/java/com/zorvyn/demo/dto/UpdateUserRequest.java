package com.zorvyn.demo.dto;

import com.zorvyn.demo.entity.Role;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateUserRequest {
    /** Optional — pass to change role. */
    private Role role;
    /** Optional — pass to change active status. */
    private Boolean active;
}
