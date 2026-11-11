package com.scm.controllers;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.scm.entities.Contact;
import com.scm.entities.User;
import com.scm.helpers.Helper;
import com.scm.services.ContactService;
import com.scm.services.EmailService;
import com.scm.services.UserService;
import com.scm.services.impl.SmsService;

@Controller
@RequestMapping("/user/messages")
public class MessageController {

    private final UserService userService;
    private final ContactService contactService;
    private final EmailService emailService;
    private final SmsService smsService;

    public MessageController(UserService userService, ContactService contactService,
            EmailService emailService, SmsService smsService) {
        this.userService = userService;
        this.contactService = contactService;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @GetMapping
    public String messages(Model model, Authentication authentication) {
        User user = currentUser(authentication);
        List<Contact> contacts = contactService.getByUserId(user.getUserId());
        model.addAttribute("contacts", contacts);
        model.addAttribute("smsConfigured", smsService.isConfigured());
        return "user/messages";
    }

    @PostMapping
    public String sendMessage(
            @RequestParam("contactId") String contactId,
            @RequestParam("message") String message,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = currentUser(authentication);
        String text = message == null ? "" : message.trim();

        if (text.isEmpty() || text.length() > 500) {
            redirectAttributes.addFlashAttribute("errorMessage", "Write a message of up to 500 characters.");
            return "redirect:/user/messages";
        }

        Contact contact;
        try {
            contact = contactService.getById(contactId);
        } catch (Exception e) {
            contact = null;
        }
        if (contact == null || contact.getUser() == null
                || !user.getUserId().equals(contact.getUser().getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose one of your contacts.");
            return "redirect:/user/messages";
        }

        boolean sent = false;
        StringBuilder result = new StringBuilder();

        if (smsService.isConfigured()
                && contact.getPhoneNumber() != null
                && !contact.getPhoneNumber().isBlank()) {
            try {
                smsService.send(contact.getPhoneNumber(), user.getName() + ": " + text);
                result.append("Text sent to ").append(contact.getPhoneNumber()).append(". ");
                sent = true;
            } catch (Exception e) {
                result.append("The phone text could not be sent. ");
            }
        }

        if (!sent && contact.getEmail() != null && !contact.getEmail().isBlank()) {
            try {
                emailService.sendEmail(
                        contact.getEmail(),
                        "Message from " + user.getName(),
                        text);
                result.append("Email sent to ").append(contact.getEmail())
                        .append(". Phone texting is not available, so the message went by email.");
                sent = true;
            } catch (Exception e) {
                result.append("The email could not be sent.");
            }
        }

        if (!sent) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This contact needs an email address, or SMS has to be configured, before a message can be sent.");
            return "redirect:/user/messages";
        }

        redirectAttributes.addFlashAttribute("successMessage", result.toString().trim());
        return "redirect:/user/messages";
    }

    private User currentUser(Authentication authentication) {
        return userService.getUserByEmail(Helper.getEmailOfLoggedInUser(authentication));
    }

}
