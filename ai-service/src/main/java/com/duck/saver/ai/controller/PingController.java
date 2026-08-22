package com.duck.saver.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

	public record PingResponse(String status) {
	}

	@GetMapping("/ping")
	public PingResponse ping() {
		return new PingResponse("pong");
	}
}
