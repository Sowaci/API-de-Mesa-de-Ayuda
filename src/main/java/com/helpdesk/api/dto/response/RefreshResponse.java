package com.helpdesk.api.dto.response;

public record RefreshResponse(
        String accessToken,
        String refreshToken
) {
}
