package com.scm.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.scm.entities.Contact;
import com.scm.entities.Reminder;
import com.scm.entities.User;
import com.scm.helpers.Helper;
import com.scm.helpers.ReminderAlert;
import com.scm.repsitories.ReminderRepo;
import com.scm.services.ContactService;
import com.scm.services.UserService;
import com.scm.services.impl.SmsService;
import java.time.ZoneId;
@Controller
@RequestMapping("/user/reminders")
public class ReminderController {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private final UserService userService;
    private final ContactService contactService;
    private final ReminderRepo reminderRepo;
    private final SmsService smsService;

    public ReminderController(UserService userService, ContactService contactService,
            ReminderRepo reminderRepo, SmsService smsService) {
        this.userService = userService;
        this.contactService = contactService;
        this.reminderRepo = reminderRepo;
        this.smsService = smsService;
    }

    @GetMapping
    public String reminders(Model model, Authentication authentication) {
        User user = currentUser(authentication);
        reminderRepo.deleteAll(
                reminderRepo.findByRemindAtLessThanEqualAndNotifiedTrueAndPopupPendingFalse(LocalDateTime.now(APP_ZONE)));
        model.addAttribute("contacts", contactService.getByUserId(user.getUserId()));
        model.addAttribute("reminders",
                reminderRepo.findByUserAndRemindAtAfterOrderByRemindAtAsc(user, LocalDateTime.now(APP_ZONE)));
        model.addAttribute("smsConfigured", smsService.isConfigured());
        return "user/reminders";
    }

    @PostMapping
    public String createReminder(
            @RequestParam("contactId") String contactId,
            @RequestParam("objective") String objective,
            @RequestParam("remindAt") String remindAt,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = currentUser(authentication);
        String goal = objective == null ? "" : objective.trim();
        if (goal.isEmpty() || goal.length() > 300) {
            redirectAttributes.addFlashAttribute("errorMessage", "Write an objective of up to 300 characters.");
            return "redirect:/user/reminders";
        }

        LocalDateTime when;
        try {
            when = LocalDateTime.parse(remindAt);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose a valid date and time.");
            return "redirect:/user/reminders";
        }
        if (!when.isAfter(LocalDateTime.now(APP_ZONE))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose a time in the future.");
            return "redirect:/user/reminders";
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
            return "redirect:/user/reminders";
        }

        Reminder reminder = new Reminder();
        reminder.setId(UUID.randomUUID().toString());
        reminder.setObjective(goal);
        reminder.setRemindAt(when);
        reminder.setUser(user);
        reminder.setContact(contact);
        reminderRepo.save(reminder);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Reminder saved. You will get a popup and an email when it is time.");
        return "redirect:/user/reminders";
    }

    @GetMapping("/pending")
    @ResponseBody
    public List<ReminderAlert> pending(Authentication authentication) {
        User user = currentUser(authentication);
        return reminderRepo.findByUserAndPopupPendingTrueOrderByRemindAtAsc(user).stream()
                .map(reminder -> {
                    Contact contact = reminder.getContact();
                    return new ReminderAlert(
                            reminder.getId(),
                            contact != null ? contact.getName() : "a contact",
                            contact != null ? contact.getPhoneNumber() : "",
                            reminder.getObjective());
                })
                .toList();
    }

    @PostMapping("/{id}/dismiss")
    @ResponseBody
    public ResponseEntity<Void> dismiss(@PathVariable String id, Authentication authentication) {
        User user = currentUser(authentication);
        Reminder reminder = reminderRepo.findById(id).orElse(null);
        if (reminder == null || reminder.getUser() == null
                || !user.getUserId().equals(reminder.getUser().getUserId())) {
            return ResponseEntity.notFound().build();
        }
        reminderRepo.delete(reminder);
        return ResponseEntity.ok().build();
    }

    private User currentUser(Authentication authentication) {
        return userService.getUserByEmail(Helper.getEmailOfLoggedInUser(authentication));
    }

}
