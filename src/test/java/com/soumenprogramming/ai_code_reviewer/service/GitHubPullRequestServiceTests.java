package com.soumenprogramming.ai_code_reviewer.service;

import com.soumenprogramming.ai_code_reviewer.config.GitHubProperties;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestCoordinates;
import com.soumenprogramming.ai_code_reviewer.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubPullRequestServiceTests {

	@Test
	void parsePullRequestUrlExtractsOwnerRepoAndNumber() {
		GitHubPullRequestService service = createServiceWithToken("token");

		PullRequestCoordinates coordinates = service.parsePullRequestUrl(
			"https://github.com/openai/demo-repo/pull/123"
		);

		assertEquals("openai", coordinates.owner());
		assertEquals("demo-repo", coordinates.repo());
		assertEquals(123L, coordinates.pullNumber());
	}

	@Test
	void fetchPullRequestReviewDataFailsWhenTokenIsMissing() {
		GitHubPullRequestService service = createServiceWithToken("");

		ApplicationException exception = assertThrows(
			ApplicationException.class,
			() -> service.fetchPullRequestReviewData("https://github.com/openai/demo-repo/pull/123")
		);

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
		assertEquals(
			"GITHUB_TOKEN is not configured. Set it before reviewing pull requests.",
			exception.getMessage()
		);
	}

	private GitHubPullRequestService createServiceWithToken(String token) {
		GitHubProperties properties = new GitHubProperties();
		properties.setToken(token);
		return new GitHubPullRequestService(HttpClient.newHttpClient(), new ObjectMapper(), properties);
	}
}
