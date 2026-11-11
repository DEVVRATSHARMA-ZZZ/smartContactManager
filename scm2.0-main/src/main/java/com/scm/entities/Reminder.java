package com.scm.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    private String id;

    @Column(length = 500, nullable = false)
    private String objective;

    private LocalDateTime remindAt;

    private boolean notified = false;

    private boolean popupPending = false;

    @ManyToOne
    private User user;

    @ManyToOne
    private Contact contact;

}
