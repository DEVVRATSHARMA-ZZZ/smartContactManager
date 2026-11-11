package com.scm.services.impl;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.from.number:}")
    private String fromNumber;

    public boolean isConfigured() {
        return !isBlank(accountSid) && !isBlank(authToken) && !isBlank(fromNumber);
    }

    public void send(String to, String body) {
        if (!isConfigured()) {
            throw new IllegalStateException("SMS is not configured");
        }
        if (isBlank(to)) {
            throw new IllegalArgumentException("Phone number is missing");
        }

        String form = "To=" + encode(to.trim())
                + "&From=" + encode(fromNumber.trim())
                + "&Body=" + encode(body);

        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("SMS provider rejected the message");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not send the text message", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
