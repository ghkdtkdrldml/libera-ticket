// com/libera/ticket/service/SmsService.java
package com.libera.ticket.service;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.service.DefaultMessageService;
import com.solapi.sdk.message.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private final DefaultMessageService messageService;
    private final String from;

    @Autowired
    public SmsService(
            @Value("${solapi.api-key}") String apiKey,
            @Value("${solapi.api-secret}") String apiSecret,
            @Value("${solapi.from}") String from
    ) {
        this.messageService = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
        this.from = from;
    }

    /** 폰 포맷: 010-1234-5678 -> 01012345678 */
    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[^0-9]", "");
    }

    /** SMS: 한글 45자(영문 90자) 이내 */
    public void sendSms(String toRaw, String text) {
        String to = normalizePhone(toRaw);
        String fromNorm = normalizePhone(from);

        Message msg = new Message();
        msg.setFrom(fromNorm);
        msg.setTo(to);
        msg.setText(text);

        try {
            messageService.send(msg);
        } catch (SolapiMessageNotReceivedException e) {
            // 실패한 건 리스트 확인 가능
            System.out.println("SMS 실패: " + e.getFailedMessageList());
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
