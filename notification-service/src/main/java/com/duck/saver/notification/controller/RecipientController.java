package com.duck.saver.notification.controller;

import com.duck.saver.notification.dto.RecipientResponse;
import com.duck.saver.notification.dto.SaveRecipientRequest;
import com.duck.saver.notification.service.RecipientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/recipients")
public class RecipientController {

	@Autowired
	private RecipientService recipientService;

	@GetMapping(path = "/current")
	public RecipientResponse getCurrentNotificationsSettings(Principal principal) {
		return recipientService.findByAccountName(principal.getName());
	}

	@PutMapping(path = "/current")
	public RecipientResponse saveCurrentNotificationsSettings(Principal principal,
			@Valid @RequestBody SaveRecipientRequest request) {
		return recipientService.save(principal.getName(), request);
	}
}
