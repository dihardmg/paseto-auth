package com.paseto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteResponse {

    private String id;

    public static DeleteResponse of(Long id) {
        return new DeleteResponse(id.toString());
    }

    public static DeleteResponse of(String id) {
        return new DeleteResponse(id);
    }
}
