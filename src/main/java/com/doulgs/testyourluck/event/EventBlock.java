package com.doulgs.testyourluck.event;

/** Motivos pelos quais o evento não aceita cadastros nem jogadas. */
public enum EventBlock {
    INACTIVE("Evento não está ativo"),
    LIMIT("Limite de sorteios atingido"),
    OUTSIDE_PERIOD("Fora do período do evento");

    private final String message;

    EventBlock(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
