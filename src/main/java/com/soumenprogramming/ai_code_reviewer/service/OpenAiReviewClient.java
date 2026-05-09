package com.soumenprogramming.ai_code_reviewer.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.soumenprogramming.ai_code_reviewer.config.AiReviewerProperties;
import com.soumenprogramming.ai_code_reviewer.exception.ApplicationException;
import org.springframework.http.HttpStatus;

import java.util.stream.Collectors;

public class OpenAiReviewClient implements AiReviewClient {

	private final OpenAIClient client;
	private final String model;

	public OpenAiReviewClient(AiReviewerProperties properties) {
		this.client = OpenAIOkHttpClient.builder()
			.apiKey(properties.getApiKey())
			.build();
		this.model = properties.getModel();
	}

	@Override
	public String review(String prompt) {
		try {
			ResponseCreateParams params = ResponseCreateParams.builder()
				.model(model)
				.input(prompt)
				.build();

			Response response = client.responses().create(params);
			return extractOutputText(response);
		} catch (RateLimitException exception) {
			throw new ApplicationException(
				HttpStatus.TOO_MANY_REQUESTS,
				"OpenAI quota or rate limit has been exceeded. Please check your API plan or try again later."
			);
		} catch (UnauthorizedException exception) {
			throw new ApplicationException(
				HttpStatus.UNAUTHORIZED,
				"OpenAI API key is invalid. Update OPENAI_API_KEY and try again."
			);
		} catch (OpenAIIoException exception) {
			throw new ApplicationException(
				HttpStatus.BAD_GATEWAY,
				"Failed to contact OpenAI. Please try again."
			);
		} catch (OpenAIServiceException exception) {
			throw new ApplicationException(
				HttpStatus.BAD_GATEWAY,
				"OpenAI request failed. Please try again later."
			);
		}
	}

	private String extractOutputText(Response response) {
		String output = response.output().stream()
			.filter(item -> item.message().isPresent())
			.flatMap(item -> item.asMessage().content().stream())
			.filter(content -> content.outputText().isPresent())
			.map(content -> content.asOutputText().text())
			.collect(Collectors.joining("\n"));

		if (output.isBlank()) {
			return "The model returned an empty response.";
		}

		return output;
	}
}
