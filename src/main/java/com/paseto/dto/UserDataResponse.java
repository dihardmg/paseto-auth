package com.paseto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDataResponse {

    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;

    public static UserDataResponse fromEntity(com.paseto.entity.User user) {
        return new UserDataResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
