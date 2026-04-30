package com.innerview.spring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SfuAccessTokenDto {
    @JsonProperty("sfu_access_token")
    String SfuAccessToken;
}
