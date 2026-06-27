package com.soumenprogramming.ai_code_reviewer.dto;

import java.util.List;

public record PullRequestDetailsResponse(
	String owner,
	String repo,
	long pullRequestNumber,
	List<PullRequestFile> files
) {
}
