package com.scm.services.impl;

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
