package com.legal.cases.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class QRCodeService {

    @Value("${qr.base-url:http://localhost:4200}")
    private String baseUrl;

    @Value("${qr.size:300}")
    private int qrSize;

    /**
     * Genera un QR code como Base64 PNG para un caso
     */
    public String generateQRCodeBase64(String caseNumber, Long caseId) {
        String content = buildQRContent(caseNumber, caseId);
        try {
            byte[] qrBytes = generateQRBytes(content);
            String base64 = Base64.getEncoder().encodeToString(qrBytes);
            log.debug("QR generado para caso: {}", caseNumber);
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException e) {
            log.error("Error generando QR para caso {}: {}", caseNumber, e.getMessage());
            throw new RuntimeException("Error generando código QR", e);
        }
    }

    /**
     * Genera los bytes del QR code PNG
     */
    public byte[] generateQRBytes(String content) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);

        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    private String buildQRContent(String caseNumber, Long caseId) {
        return String.format("%s/cases/%d?ref=%s", baseUrl, caseId, caseNumber);
    }
}
