package com.soumenprogramming.ai_code_reviewer.dto;

import java.util.List;

public record PullRequestReviewData(
	PullRequestCoordinates coordinates,
	String prUrl,
	List<PullRequestFile> files
) {
}
