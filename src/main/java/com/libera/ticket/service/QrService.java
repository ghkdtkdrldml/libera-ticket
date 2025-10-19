// com/libera/ticket/service/QrService.java
package com.libera.ticket.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

@Service
public class QrService {
    public BufferedImage generatePng(String content, int size){
        try {
            var hints = new Hashtable<EncodeHintType, Object>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            var bit = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            return MatrixToImageWriter.toBufferedImage(bit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
