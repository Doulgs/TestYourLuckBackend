package com.doulgs.testyourluck.webhook;

public final class WebhookEnums {

    private WebhookEnums() {
    }

    public enum Trigger { LEAD, PLAY }

    public enum Method { GET, POST, PUT, PATCH, DELETE }
}
