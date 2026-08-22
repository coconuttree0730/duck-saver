package com.duck.saver.statistics;

import com.duck.saver.statistics.repository.converter.DataPointIdReaderConverter;
import com.duck.saver.statistics.repository.converter.DataPointIdWriterConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@SpringBootApplication
@EnableFeignClients
public class StatisticsApplication {

	public static void main(String[] args) {
		SpringApplication.run(StatisticsApplication.class, args);
	}

	@Configuration
	static class CustomConversionsConfig {

		@Bean
		public MongoCustomConversions customConversions() {
			return new MongoCustomConversions(List.of(new DataPointIdReaderConverter(),
					new DataPointIdWriterConverter()));
		}
	}
}
