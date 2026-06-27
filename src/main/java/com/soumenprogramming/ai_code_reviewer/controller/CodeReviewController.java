package com.soumenprogramming.ai_code_reviewer.controller;

import com.soumenprogramming.ai_code_reviewer.dto.CodeReviewRequest;
import com.soumenprogramming.ai_code_reviewer.dto.CodeReviewResponse;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestDetailsResponse;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestReviewData;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestReviewRequest;
import com.soumenprogramming.ai_code_reviewer.dto.RuleBasedPullRequestReviewRequest;
import com.soumenprogramming.ai_code_reviewer.service.CodeReviewService;
import com.soumenprogramming.ai_code_reviewer.service.GitHubPullRequestService;
import com.soumenprogramming.ai_code_reviewer.service.PullRequestReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class CodeReviewController {

	private static final Logger log = LoggerFactory.getLogger(CodeReviewController.class);

	private final CodeReviewService codeReviewService;
	private final PullRequestReviewService pullRequestReviewService;
	private final GitHubPullRequestService gitHubPullRequestService;

	public CodeReviewController(CodeReviewService codeReviewService,
			PullRequestReviewService pullRequestReviewService,
			GitHubPullRequestService gitHubPullRequestService) {
		this.codeReviewService = codeReviewService;
		this.pullRequestReviewService = pullRequestReviewService;
		this.gitHubPullRequestService = gitHubPullRequestService;
	}

	@PostMapping("/review")
	@ResponseStatus(HttpStatus.OK)
	public CodeReviewResponse review(@RequestBody CodeReviewRequest request) {
		validateCodeReviewRequest(request);
		String review = codeReviewService.reviewCode(request);
		return new CodeReviewResponse(review);
	}

	@PostMapping("/review-pr")
	@ResponseStatus(HttpStatus.OK)
	public CodeReviewResponse reviewPullRequest(@RequestBody PullRequestReviewRequest request) {
		validatePullRequestReviewRequest(request);
		String review = pullRequestReviewService.reviewPullRequest(request.prUrl());
		return new CodeReviewResponse(review);
	}

	@GetMapping("/reviews/pull-request")
	@ResponseStatus(HttpStatus.OK)
	public CodeReviewResponse reviewPullRequestWithRules(@RequestBody RuleBasedPullRequestReviewRequest request) {
		validateRuleBasedPullRequestReviewRequest(request);
		log.info("Reviewing PR with rule packs {}: {}", request.rulePacks(), request.prUrl());
		String review = pullRequestReviewService.reviewPullRequestWithRules(
				request.prUrl(),
				request.rulePacks()
		);
		return new CodeReviewResponse(review);
	}

	@PostMapping("/fetch-pr")
	@ResponseStatus(HttpStatus.OK)
	public PullRequestDetailsResponse fetchPullRequest(@RequestBody PullRequestReviewRequest request) {
		validatePullRequestReviewRequest(request);
		PullRequestReviewData reviewData = gitHubPullRequestService.fetchPullRequestReviewData(request.prUrl());
		return new PullRequestDetailsResponse(
				reviewData.coordinates().owner(),
				reviewData.coordinates().repo(),
				reviewData.coordinates().pullNumber(),
				reviewData.files()
		);
	}

	private void validateCodeReviewRequest(CodeReviewRequest request) {
		if (request == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
		}

		if (request.language() == null || request.language().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Language is required.");
		}

		if (request.code() == null || request.code().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code is required.");
		}
	}

	private void validatePullRequestReviewRequest(PullRequestReviewRequest request) {
		if (request == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
		}

		if (request.prUrl() == null || request.prUrl().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pull request URL is required.");
		}
	}

	private void validateRuleBasedPullRequestReviewRequest(RuleBasedPullRequestReviewRequest request) {
		if (request == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
		}

		if (request.prUrl() == null || request.prUrl().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pull request URL is required.");
		}

		if (request.rulePacks() == null || request.rulePacks().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one rule pack is required.");
		}
	}
}
