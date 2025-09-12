package com.libera.ticket.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.*;

public record CreateApplicationReq(

        @NotBlank String domainType,   // RSVP | INVITE
        String performerName,          // INVITE일 필수

        boolean isRepDelivery,

        @Valid
        @Size(min = 1, max = 6) List<MemberReq> members
) {}
