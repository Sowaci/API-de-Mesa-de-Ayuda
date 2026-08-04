package com.helpdesk.api.dto;

public record RefreshResponse(
        String accessToken,
        String refreshToken
) {
}
