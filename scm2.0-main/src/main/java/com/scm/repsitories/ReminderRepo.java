package com.scm.repsitories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scm.entities.Contact;
import com.scm.entities.Reminder;
import com.scm.entities.User;

public interface ReminderRepo extends JpaRepository<Reminder, String> {

    List<Reminder> findByNotifiedFalseAndRemindAtLessThanEqual(LocalDateTime time);

    List<Reminder> findByUserAndPopupPendingTrueOrderByRemindAtAsc(User user);

    List<Reminder> findByUserOrderByRemindAtAsc(User user);

    List<Reminder> findByUserAndRemindAtAfterOrderByRemindAtAsc(User user, LocalDateTime time);

    List<Reminder> findByRemindAtLessThanEqualAndNotifiedTrueAndPopupPendingFalse(LocalDateTime time);

    List<Reminder> findByContact(Contact contact);

}
