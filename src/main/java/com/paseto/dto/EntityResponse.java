package com.paseto.dto;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Hidden
public class EntityResponse<T> {

    private Integer status;
    private String message;
    private T data;

    public static <T> EntityResponse<T> of(Integer status, String message, T data) {
        return new EntityResponse<>(status, message, data);
    }
}
