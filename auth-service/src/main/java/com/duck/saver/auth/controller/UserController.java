package com.duck.saver.auth.controller;

import com.duck.saver.auth.domain.User;
import com.duck.saver.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/users")
public class UserController {

	@Autowired
	private UserService userService;

	@GetMapping(value = "/current")
	public Principal getUser(Principal principal) {
		return principal;
	}

	@PostMapping
	public void createUser(@Valid @RequestBody User user) {
		userService.create(user);
	}
}
