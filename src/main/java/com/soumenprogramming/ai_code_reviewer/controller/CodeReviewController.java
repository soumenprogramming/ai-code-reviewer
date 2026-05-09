package com.soumenprogramming.ai_code_reviewer.controller;

import com.soumenprogramming.ai_code_reviewer.dto.CodeReviewRequest;
import com.soumenprogramming.ai_code_reviewer.dto.CodeReviewResponse;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestReviewRequest;
import com.soumenprogramming.ai_code_reviewer.service.CodeReviewService;
import com.soumenprogramming.ai_code_reviewer.service.PullRequestReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class CodeReviewController {

	private final CodeReviewService codeReviewService;
	private final PullRequestReviewService pullRequestReviewService;

	public CodeReviewController(CodeReviewService codeReviewService,
			PullRequestReviewService pullRequestReviewService) {
		this.codeReviewService = codeReviewService;
		this.pullRequestReviewService = pullRequestReviewService;
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
}
