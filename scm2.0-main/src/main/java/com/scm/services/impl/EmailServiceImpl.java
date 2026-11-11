/*package com.scm.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

import com.scm.services.EmailService;

@Service
public class EmailServiceImpl implements EmailService {

   // @Autowired
  //  private JavaMailSender eMailSender;

  //  @Value("${spring.mail.properties.domain_name}")
  //  private String domainName;

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from}")
    private String fromEmail;

    private final RestClient restClient = RestClient.create();

//    @Override
//    public void sendEmail(String to, String subject, String body) {
//
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(to);
//        message.setSubject(subject);
//        message.setText(body);
//        message.setFrom(domainName);
//        eMailSender.send(message);
//
//    }

    @Override
    public void sendEmail(String to, String subject, String body) {

        restClient.post()
                .uri("https://api.resend.com/emails")
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .body(Map.of(
                        "from", fromEmail,
                        "to", List.of(to),
                        "subject", subject,
                        "text", body
                ))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void sendEmailWithHtml() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendEmailWithHtml'");
    }

    @Override
    public void sendEmailWithAttachment() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendEmailWithAttachment'");
    }

}
*/

package com.scm.services.impl;

import com.scm.services.EmailService;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${GMAIL_CLIENT_ID}")
    private String clientId;

    @Value("${GMAIL_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${GMAIL_REFRESH_TOKEN}")
    private String refreshToken;

    @Value("${GMAIL_SENDER_EMAIL}")
    private String senderEmail;

    private final RestClient restClient = RestClient.create();

    @Override
    public void sendEmail(String to, String subject, String body) {

        try {
            // Step 1: Get a new access token using the refresh token
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("refresh_token", refreshToken);
            form.add("grant_type", "refresh_token");

            Map<String, Object> tokenResponse = restClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (tokenResponse == null || tokenResponse.get("access_token") == null) {
                throw new RuntimeException("Could not obtain Gmail access token");
            }

            String accessToken = (String) tokenResponse.get("access_token");

            // Step 2: Create the email message
            Session session = Session.getInstance(new Properties());
            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");
            message.saveChanges();

            // Step 3: Encode the email for Gmail API
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);

            String rawEmail = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(outputStream.toByteArray());

            // Step 4: Send email using Gmail API
            restClient.post()
                    .uri("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("raw", rawEmail))
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email using Gmail API", e);
        }
    }

    @Override
    public void sendEmailWithHtml() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void sendEmailWithAttachment() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
