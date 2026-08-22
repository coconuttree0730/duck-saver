package com.duck.saver.auth.service;

import com.duck.saver.auth.domain.User;
import com.duck.saver.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Autowired
	private UserRepository repository;

	@Override
	public void create(User user) {

		repository.findById(user.getUsername()).ifPresent(it -> {
			throw new IllegalArgumentException("user already exists: " + it.getUsername());
		});

		String hash = encoder.encode(user.getPassword());
		user.setPassword(hash);

		repository.save(user);

		log.info("new user has been created: {}", user.getUsername());
	}

	@Override
	public User authenticate(String username, String password) {

		User user = repository.findById(username)
				.orElseThrow(() -> new IllegalArgumentException("invalid credentials"));

		if (!encoder.matches(password, user.getPassword())) {
			throw new IllegalArgumentException("invalid credentials");
		}

		return user;
	}
}
