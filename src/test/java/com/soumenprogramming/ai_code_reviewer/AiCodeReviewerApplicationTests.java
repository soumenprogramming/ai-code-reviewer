package com.soumenprogramming.ai_code_reviewer;

import com.soumenprogramming.ai_code_reviewer.config.GitHubProperties;
import com.soumenprogramming.ai_code_reviewer.controller.CodeReviewController;
import com.soumenprogramming.ai_code_reviewer.dto.CodeReviewRequest;
import com.soumenprogramming.ai_code_reviewer.dto.CodeReviewResponse;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestReviewRequest;
import com.soumenprogramming.ai_code_reviewer.service.CodeReviewService;
import com.soumenprogramming.ai_code_reviewer.service.GitHubPullRequestService;
import com.soumenprogramming.ai_code_reviewer.service.LanguageDetectionService;
import com.soumenprogramming.ai_code_reviewer.service.PlaceholderAiReviewClient;
import com.soumenprogramming.ai_code_reviewer.service.PullRequestReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AiCodeReviewerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void reviewEndpointReturnsPlaceholderReview() {
		CodeReviewController controller = createController();
		CodeReviewResponse response = controller.review(new CodeReviewRequest("Java", "public class Demo { }"));

		assertTrue(response.review().contains("Summary:"));
	}

	@Test
	void reviewEndpointRejectsBlankCode() {
		CodeReviewController controller = createController();

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
			() -> controller.review(new CodeReviewRequest("Java", "")));

		assertEquals(400, exception.getStatusCode().value());
	}

	@Test
	void reviewPullRequestEndpointRejectsBlankUrl() {
		CodeReviewController controller = createController();

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
			() -> controller.reviewPullRequest(new PullRequestReviewRequest("")));

		assertEquals(400, exception.getStatusCode().value());
	}

	private CodeReviewController createController() {
		CodeReviewService codeReviewService = new CodeReviewService(
			new PlaceholderAiReviewClient(),
			new LanguageDetectionService()
		);
		GitHubProperties gitHubProperties = new GitHubProperties();
		GitHubPullRequestService gitHubPullRequestService = new GitHubPullRequestService(
			HttpClient.newHttpClient(),
			new ObjectMapper(),
			gitHubProperties
		);
		PullRequestReviewService pullRequestReviewService = new PullRequestReviewService(
			gitHubPullRequestService,
			codeReviewService
		);
		return new CodeReviewController(codeReviewService, pullRequestReviewService);
	}
}
