package com.vayuratha.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final RestTemplate restTemplate;
    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.access-token}")
    private String accessToken;

    @Value("${whatsapp.admin-number}")
    private String adminNumber;

    public void sendCredentials(
            String mobile,
            String name,
            String username,
            String temporaryPassword
    ) {

        try {
            String cleanMobile = mobile.replaceAll("\\D", "");
            if (cleanMobile.length() == 10) {cleanMobile = "91" + cleanMobile;}

            String url = "https://graph.facebook.com/v21.0/" + phoneNumberId + "/messages";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            Map<String, Object> template = new HashMap<>();
            template.put("name", "vayuratha_account_credentials");
            template.put("language", Map.of("code", "en"));
            template.put(
                    "components", List.of(Map.of("type", "body", "parameters",
                            List.of(
                                    Map.of("type", "text", "text", name),
                                    Map.of("type", "text", "text", username),
                                    Map.of("type", "text", "text", temporaryPassword)
                            ))
                    )
            );
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("to", cleanMobile);
            requestBody.put("type", "template");
            requestBody.put("template", template);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("======================================");
            System.out.println("WHATSAPP CREDENTIALS SENT");
            System.out.println("TO       : " + cleanMobile);
            System.out.println("USERNAME : " + username);
            System.out.println("RESPONSE : " + response.getBody());
            System.out.println("======================================");
        } catch (HttpClientErrorException e) {
            System.out.println("======================================");
            System.out.println("WHATSAPP SEND FAILED");
            System.out.println("STATUS : " + e.getStatusCode());
            System.out.println("ERROR  : " + e.getResponseBodyAsString());
            System.out.println("======================================");
        } catch (Exception e) {
            System.out.println("======================================");
            System.out.println("WHATSAPP SEND FAILED");
            System.out.println("ERROR : " + e.getMessage());
            System.out.println("======================================");
        }
    }


    public void sendResultImageToAdmin(byte[] imageBytes, String caption) {
        try {
            String mediaId = uploadMedia(imageBytes);
            if (mediaId == null) {
                System.out.println("WHATSAPP RESULT IMAGE SEND FAILED: media upload returned no id");
                return;
            }

            String url = "https://graph.facebook.com/v21.0/" + phoneNumberId + "/messages";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> imagePayload = new HashMap<>();
            imagePayload.put("id", mediaId);
            imagePayload.put("caption", caption);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("to", adminNumber);
            requestBody.put("type", "image");
            requestBody.put("image", imagePayload);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            System.out.println("======================================");
            System.out.println("WHATSAPP RESULT IMAGE SENT");
            System.out.println("TO       : " + adminNumber);
            System.out.println("RESPONSE : " + response.getBody());
            System.out.println("======================================");
        } catch (HttpClientErrorException e) {
            System.out.println("======================================");
            System.out.println("WHATSAPP RESULT IMAGE SEND FAILED");
            System.out.println("STATUS : " + e.getStatusCode());
            System.out.println("ERROR  : " + e.getResponseBodyAsString());
            System.out.println("======================================");
        } catch (Exception e) {
            System.out.println("======================================");
            System.out.println("WHATSAPP RESULT IMAGE SEND FAILED");
            System.out.println("ERROR : " + e.getMessage());
            System.out.println("======================================");
        }
    }

    private String uploadMedia(byte[] imageBytes) {
        String url = "https://graph.facebook.com/v21.0/" + phoneNumberId + "/media";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(accessToken);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "result.png";
            }
        });
        body.add("type", "image/png");
        body.add("messaging_product", "whatsapp");

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getBody() != null) {
            return (String) response.getBody().get("id");
        }
        return null;
    }
}