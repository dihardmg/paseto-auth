package com.paseto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BannerListResponse {

    private Integer code;
    private String status;
    private List<BannerResponse> data;
}
