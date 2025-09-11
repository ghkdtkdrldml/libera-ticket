package com.libera.ticket.api.dto;

public record CreateApplicationRes(
        String applicationId, int totalCount, String cancelUrl, String message
) {}
