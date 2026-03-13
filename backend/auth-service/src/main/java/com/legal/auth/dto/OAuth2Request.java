package com.legal.auth.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OAuth2Request {
    private String code;
    private String state;
    private String provider;    // google, github
    private String deviceId;
    private String deviceName;
    private String deviceType;
}
