package com.duck.saver.account.controller;

import com.duck.saver.account.dto.AccountResponse;
import com.duck.saver.account.dto.CreateAccountRequest;
import com.duck.saver.account.dto.TransactionItemRequest;
import com.duck.saver.account.dto.UpdateAccountRequest;
import com.duck.saver.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
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

	@GetMapping(path = "/accounts/current")
	public AccountResponse getCurrentAccount(Principal principal) {
		return accountService.findByName(principal.getName());
	}

	@PostMapping(path = "/accounts")
	public AccountResponse createNewAccount(@Valid @RequestBody CreateAccountRequest request) {
		return accountService.create(request);
	}

	@GetMapping(path = "/accounts/demo")
	public AccountResponse getDemoAccount() {
		return accountService.demo();
	}

	@GetMapping(path = "/accounts/{name}")
	public AccountResponse getAccountByName(@PathVariable String name) {
		return accountService.findByName(name);
	}

	@PutMapping(path = "/accounts/{name}")
	public void updateAccount(@PathVariable String name, @Valid @RequestBody UpdateAccountRequest request) {
		accountService.update(name, request);
	}

	@DeleteMapping(path = "/accounts/{name}")
	public void deleteAccount(@PathVariable String name) {
		accountService.delete(name);
	}

	@PostMapping(path = "/accounts/{name}/items")
	public void addItem(@PathVariable String name, @Valid @RequestBody TransactionItemRequest request) {
		accountService.addItem(name, request);
	}

	@DeleteMapping(path = "/accounts/{name}/items/{itemId}")
	public void deleteItem(@PathVariable String name, @PathVariable String itemId) {
		accountService.deleteItem(name, itemId);
	}
}
