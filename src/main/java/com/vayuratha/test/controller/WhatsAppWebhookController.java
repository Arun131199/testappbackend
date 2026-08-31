package com.vayuratha.test.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppWebhookController {

    @Value("${whatsapp.webhook.verify-token}")
    private String verifyToken;

    // Meta webhook verification
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {

        if ("subscribe".equals(mode)
                && verifyToken.equals(token)) {

            return ResponseEntity.ok(challenge);
        }

        System.out.println(challenge);
        System.out.println(verifyToken);
        System.out.println(verifyToken.equals(token));

        return ResponseEntity.status(403).body("Forbidden");
    }

    // WhatsApp events
    @PostMapping("/webhook")
    public ResponseEntity<String> receiveWebhook(
            @RequestBody String payload
    ) {

        System.out.println("WhatsApp Webhook Payload:");
        System.out.println(payload);

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}