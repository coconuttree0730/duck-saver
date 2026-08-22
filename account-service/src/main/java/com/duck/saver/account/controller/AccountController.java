package com.duck.saver.account.controller;

import com.duck.saver.account.domain.Account;
import com.duck.saver.account.domain.User;
import com.duck.saver.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class AccountController {

	@Autowired
	private AccountService accountService;

	@GetMapping(path = "/{name}")
	public Account getAccountByName(@PathVariable String name) {
		return accountService.findByName(name);
	}

	@GetMapping(path = "/current")
	public Account getCurrentAccount(Principal principal) {
		return accountService.findByName(principal.getName());
	}

	@PutMapping(path = "/current")
	public void saveCurrentAccount(Principal principal, @Valid @RequestBody Account account) {
		accountService.saveChanges(principal.getName(), account);
	}

	@PostMapping(path = "/")
	public Account createNewAccount(@Valid @RequestBody User user) {
		return accountService.create(user);
	}
}
