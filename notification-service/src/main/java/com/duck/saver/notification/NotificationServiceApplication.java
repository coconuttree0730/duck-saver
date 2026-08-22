package com.duck.saver.notification;

import com.duck.saver.notification.repository.converter.FrequencyReaderConverter;
import com.duck.saver.notification.repository.converter.FrequencyWriterConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

	@Configuration
	static class CustomConversionsConfig {

		@Bean
		public MongoCustomConversions customConversions() {
			return new MongoCustomConversions(List.of(new FrequencyReaderConverter(),
					new FrequencyWriterConverter()));
		}
	}
}
