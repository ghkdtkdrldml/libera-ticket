package com.libera.ticket.web;

import com.libera.ticket.service.QrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class QrController {

    private final QrService qrService;

    /**
     * 예: GET /qr/{token}.png?s=480
     * 메일/문자에서 공개 URL로 사용.
     */
    @GetMapping(value = "/qr/{token}.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrPng(
            @PathVariable String token,
            @RequestParam(name = "s", defaultValue = "480") int size
    ) {
        // 토큰으로 티켓 상세 링크를 구성
        String ticketUrl = "/t/" + token; // 절대 URL 필요하면 필터/링크빌더로 바꿔도 됨

        try (var baos = new ByteArrayOutputStream()) {
            ImageIO.write(qrService.generatePng(ticketUrl, size), "png", baos);
            byte[] png = baos.toByteArray();

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .header(HttpHeaders.PRAGMA, "public")
                    .body(png);
        } catch (Exception e) {
            return ResponseEntity.notFound().build(); // 필요시 400/500으로 조정
        }
    }
}
