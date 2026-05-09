package com.soumenprogramming.ai_code_reviewer.service;

import com.soumenprogramming.ai_code_reviewer.dto.PullRequestReviewData;
import org.springframework.stereotype.Service;

@Service
public class PullRequestReviewService {

	private final GitHubPullRequestService gitHubPullRequestService;
	private final CodeReviewService codeReviewService;

	public PullRequestReviewService(GitHubPullRequestService gitHubPullRequestService,
			CodeReviewService codeReviewService) {
		this.gitHubPullRequestService = gitHubPullRequestService;
		this.codeReviewService = codeReviewService;
	}

	public String reviewPullRequest(String prUrl) {
		PullRequestReviewData reviewData = gitHubPullRequestService.fetchPullRequestReviewData(prUrl);
		return codeReviewService.reviewPullRequest(reviewData);
	}
}
