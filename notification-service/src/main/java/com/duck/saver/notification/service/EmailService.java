package com.duck.saver.notification.service;

import com.duck.saver.notification.domain.NotificationType;
import com.duck.saver.notification.domain.Recipient;

import jakarta.mail.MessagingException;
import java.io.IOException;

public interface EmailService {

	void send(NotificationType type, Recipient recipient, String attachment) throws MessagingException, IOException;

}
