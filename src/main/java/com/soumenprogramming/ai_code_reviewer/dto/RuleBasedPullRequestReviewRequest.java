package com.soumenprogramming.ai_code_reviewer.dto;

import java.util.List;

public record RuleBasedPullRequestReviewRequest(
	String prUrl,
	List<RulePack> rulePacks
) {
}
