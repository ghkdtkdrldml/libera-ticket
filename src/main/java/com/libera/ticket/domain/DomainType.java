package com.libera.ticket.domain;
public enum DomainType {
    RSVP("일반 응모"),
    INVITE("초대권 수령");

    private final String label;
    DomainType(String label){ this.label = label; }
    public String ko(){ return label; }
}