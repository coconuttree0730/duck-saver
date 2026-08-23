package com.duck.saver.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.auth.domain.User;
import com.duck.saver.auth.entity.UserEntity;
import com.duck.saver.auth.mapper.UserMapper;
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
	private UserMapper userMapper;

	@Override
	public void create(User user) {

		if ((userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
				.eq(UserEntity::getUsername, user.getUsername()))).size() > 0) {
			throw new IllegalArgumentException("user already exists: " + user.getUsername());
		}

		UserEntity entity = new UserEntity();
		entity.setUsername(user.getUsername());
		entity.setPassword(encoder.encode(user.getPassword()));
		userMapper.insert(entity);

		log.info("new user has been created: {}", entity.getUsername());
	}

	@Override
	public User authenticate(String username, String password) {

		UserEntity entity = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
				.eq(UserEntity::getUsername, username));
		if (entity == null || !encoder.matches(password, entity.getPassword())) {
			throw new IllegalArgumentException("invalid credentials");
		}

		User user = new User();
		user.setUsername(entity.getUsername());
		return user;
	}
}
