package com.scm.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scm.entities.Contact;
import com.scm.entities.Reminder;
import com.scm.entities.User;
import com.scm.repsitories.ReminderRepo;
import com.scm.services.EmailService;
import java.time.ZoneId;

@Service
public class ReminderService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private Logger logger = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderRepo reminderRepo;
    private final EmailService emailService;
    private final SmsService smsService;

    public ReminderService(ReminderRepo reminderRepo, EmailService emailService, SmsService smsService) {
        this.reminderRepo = reminderRepo;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void dispatchDueReminders() {
        List<Reminder> due = reminderRepo.findByNotifiedFalseAndRemindAtLessThanEqual(LocalDateTime.now(APP_ZONE));
        for (Reminder reminder : due) {
            notifyOwner(reminder);
            reminder.setNotified(true);
            reminder.setPopupPending(true);
            reminderRepo.save(reminder);
        }
        reminderRepo.deleteAll(
                reminderRepo.findByRemindAtLessThanEqualAndNotifiedTrueAndPopupPendingFalse(LocalDateTime.now(APP_ZONE)));
    }

    private void notifyOwner(Reminder reminder) {
        User user = reminder.getUser();
        if (user == null) {
            return;
        }

        Contact contact = reminder.getContact();
        String contactName = contact != null && contact.getName() != null ? contact.getName() : "a contact";
        String phone = contact != null && contact.getPhoneNumber() != null ? contact.getPhoneNumber() : "not saved";
        String objective = reminder.getObjective() == null ? "" : reminder.getObjective();

        String emailBody = "Time to call " + contactName + ".\n"
                + "Phone: " + phone + "\n"
                + "Objective: " + objective;

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendEmail(user.getEmail(), "Call reminder: " + contactName, emailBody);
            } catch (Exception e) {
                logger.warn("Could not email reminder to {}", user.getEmail());
            }
        }

        if (smsService.isConfigured() && user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            try {
                String text = "SCM reminder: Call " + contactName + " (" + phone + "). " + objective;
                if (text.length() > 300) {
                    text = text.substring(0, 300);
                }
                smsService.send(user.getPhoneNumber(), text);
            } catch (Exception e) {
                logger.warn("Could not text reminder to the account phone");
            }
        }
    }

}
