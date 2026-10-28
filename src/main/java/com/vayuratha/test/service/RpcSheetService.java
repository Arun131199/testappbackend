package com.vayuratha.test.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RpcSheetService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${rpc.sheet.webhook-url}")
    private String sheetWebhookUrl;

    public void appendBooking(
            String enquiryId,
            String name,
            String email,
            String mobile,
            String qualification,
            String address,
            String slot,
            String trainingDate,
            String paymentId,
            String orderId
    ) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("enquiryId", enquiryId);
            payload.put("name", name);
            payload.put("email", email);
            payload.put("mobile", mobile);
            payload.put("qualification", qualification);
            payload.put("address", address);
            payload.put("slot", slot);
            payload.put("trainingDate", trainingDate);
            payload.put("paymentId", paymentId);
            payload.put("orderId", orderId);

            String jsonBody = MAPPER.writeValueAsString(payload);
            int status = postFollowingRedirects(sheetWebhookUrl, jsonBody, 0);

            System.out.println("======================================");
            System.out.println(status == 200 ? "RPC SHEET SYNC SUCCESS" : "RPC SHEET SYNC FAILED (status " + status + ")");
            System.out.println("ENQUIRY : " + enquiryId);
            System.out.println("======================================");
        } catch (Exception e) {
            System.out.println("======================================");
            System.out.println("RPC SHEET SYNC FAILED");
            System.out.println("ENQUIRY : " + enquiryId);
            System.out.println("ERROR   : " + e.getMessage());
            System.out.println("======================================");
        }
    }

    private int postFollowingRedirects(String urlStr, String jsonBody, int depth) throws Exception {
        if (depth > 5) {
            throw new RuntimeException("Too many redirects");
        }

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();

        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
            String location = conn.getHeaderField("Location");
            conn.disconnect();
            if (location == null) {
                throw new RuntimeException("Redirect with no Location header");
            }
            return postFollowingRedirects(location, jsonBody, depth + 1);
        }

        conn.disconnect();
        return status;
    }
}