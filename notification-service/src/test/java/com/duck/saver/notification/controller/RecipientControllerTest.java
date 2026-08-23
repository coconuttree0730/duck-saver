package com.duck.saver.notification.controller;

import java.util.List;
import com.duck.saver.notification.dto.NotificationConfigResponse;
import com.duck.saver.notification.dto.RecipientInfoResponse;
import com.duck.saver.notification.dto.RecipientResponse;
import com.duck.saver.notification.dto.SaveRecipientRequest;
import com.duck.saver.notification.service.RecipientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.security.auth.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecipientControllerTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	@InjectMocks
	private RecipientController recipientController;

	@Mock
	private RecipientService recipientService;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		this.mockMvc = MockMvcBuilders.standaloneSetup(recipientController)
				.setControllerAdvice(new com.duck.saver.common.web.GlobalExceptionHandler(),
						new com.duck.saver.common.web.ResultWrapAdvice(mapper))
				.build();
	}

	@Test
	public void shouldGetCurrentRecipientSettings() throws Exception {

		when(recipientService.findByAccountName("test")).thenReturn(stubResponse());

		mockMvc.perform(get("/recipients/current").principal(new UserPrincipal("test")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.recipient.name").value("test"))
				.andExpect(jsonPath("$.data.recipient.frequency").value("WEEKLY"))
				.andExpect(jsonPath("$.data.notificationConfig[0].type").value("BACKUP"));
	}

	@Test
	public void shouldSaveCurrentRecipientSettings() throws Exception {

		when(recipientService.save(eq("test"), any(SaveRecipientRequest.class))).thenReturn(stubResponse());

		String json = """
				{ "email": "test@example.com", "frequency": "WEEKLY", "enabled": true }
				""";

		mockMvc.perform(put("/recipients/current").principal(new UserPrincipal("test"))
						.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.recipient.email").value("test@example.com"));
	}

	private RecipientResponse stubResponse() {

		RecipientInfoResponse info = new RecipientInfoResponse();
		info.setName("test");
		info.setEmail("test@example.com");
		info.setFrequency("WEEKLY");
		info.setEnabled(true);

		RecipientResponse response = new RecipientResponse();
		response.setRecipient(info);
		response.setNotificationConfig(List.of(
				new NotificationConfigResponse("BACKUP", "0 0 12 * * *"),
				new NotificationConfigResponse("BILL_REMINDER", "0 0 10 1 * *")));
		return response;
	}
}
