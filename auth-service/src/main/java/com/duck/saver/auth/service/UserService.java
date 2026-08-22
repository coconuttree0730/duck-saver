package com.duck.saver.auth.service;

import com.duck.saver.auth.domain.User;

public interface UserService {

	void create(User user);

	User authenticate(String username, String password);
}
