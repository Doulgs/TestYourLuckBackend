package com.doulgs.testyourluck.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Proteção contra SSRF: só HTTPS e nunca para loopback, rede privada, link-local, multicast ou ULA IPv6.
 * {@code app.webhooks.allow-insecure-targets} libera destinos locais apenas em testes.
 */
@Component
public class WebhookTargetGuard {

    static final String NOT_ALLOWED = "URL inválida ou não permitida";

    private final boolean allowInsecure;

    public WebhookTargetGuard(@Value("${app.webhooks.allow-insecure-targets:false}") boolean allowInsecure) {
        this.allowInsecure = allowInsecure;
    }

    public URI check(String url) {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(NOT_ALLOWED);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (uri.getHost() == null || !(scheme.equals("https") || (allowInsecure && scheme.equals("http")))) {
            throw new IllegalArgumentException(NOT_ALLOWED);
        }
        if (allowInsecure) {
            return uri;
        }
        try {
            for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
                if (forbidden(a)) {
                    throw new IllegalArgumentException(NOT_ALLOWED);
                }
            }
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Host não encontrado: " + uri.getHost());
        }
        return uri;
    }

    static boolean forbidden(InetAddress a) {
        if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress() || a.isAnyLocalAddress()
                || a.isMulticastAddress()) {
            return true;
        }
        // fc00::/7 (Unique Local Address) e 100.64.0.0/10 (CGNAT)
        byte[] b = a.getAddress();
        if (a instanceof Inet6Address) {
            return (b[0] & 0xFE) == 0xFC;
        }
        return (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64;
    }
}
