package com.libera.ticket.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.*;

public record CreateApplicationReq(

        @NotBlank String domainType,   // RSVP | INVITE
        String performerName,          // INVITE일 필수

        boolean isRepDelivery,

        // ✅ 프런트에서 체크해야만 true로 전송. 서버에서 반드시 true 요구
        @AssertTrue(message = "개인정보 제공에 동의해 주세요.")
        boolean consentAgreed,

        @Valid
        @Size(min = 1, max = 6) List<MemberReq> members
) {}
