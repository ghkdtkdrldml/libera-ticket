package com.libera.ticket.domain;
public enum AppStatus {
    SUBMITTED("접수됨"),
    CANCELED("취소됨");

    private final String label;
    AppStatus(String label){ this.label = label; }
    public String ko(){ return label; }
}