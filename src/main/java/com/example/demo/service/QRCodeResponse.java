package com.example.demo.service;

public class QRCodeResponse {
    private Long qrCodeId;
    private String qrImage;

    public QRCodeResponse(Long qrCodeId, String qrImage) {
        this.qrCodeId = qrCodeId;
        this.qrImage = qrImage;
    }

    public Long getQrCodeId() {
        return qrCodeId;
    }

    public String getQrImage() {
        return qrImage;
    }
}
