package com.soumenprogramming.ai_code_reviewer.dto;

public record PullRequestFile(
	String filename,
	String status,
	int additions,
	int deletions,
	String patch
) {
}
