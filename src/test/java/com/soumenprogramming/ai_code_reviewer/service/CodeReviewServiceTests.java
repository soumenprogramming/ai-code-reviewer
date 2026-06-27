package com.soumenprogramming.ai_code_reviewer.service;

import com.soumenprogramming.ai_code_reviewer.dto.PullRequestCoordinates;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestFile;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestReviewData;
import com.soumenprogramming.ai_code_reviewer.dto.RulePack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeReviewServiceTests {

	@Test
	void pullRequestPromptIncludesDetectedLanguagesAndRelevantGuidance() {
		ReviewRulePackService reviewRulePackService = new ReviewRulePackService();
		CodeReviewService service = new CodeReviewService(
			new PlaceholderAiReviewClient(),
			new LanguageDetectionService(),
			reviewRulePackService
		);

		String prompt = service.buildPullRequestReviewPrompt(new PullRequestReviewData(
			new PullRequestCoordinates("owner", "repo", 123),
			"https://github.com/owner/repo/pull/123",
			List.of(
				new PullRequestFile("src/main/java/App.java", "modified", 3, 1, "@@ java patch"),
				new PullRequestFile("web/src/App.tsx", "modified", 8, 2, "@@ tsx patch"),
				new PullRequestFile("db/migration.sql", "added", 4, 0, "@@ sql patch"),
				new PullRequestFile("package.json", "modified", 1, 1, "@@ node patch")
			)
		));

		assertTrue(prompt.contains("Detected languages: Java (1 file), TypeScript React (1 file), SQL (1 file), JSON / Node.js (1 file)"));
		assertTrue(prompt.contains("Detected language: TypeScript React"));
		assertTrue(prompt.contains("For JVM/Spring files"));
		assertTrue(prompt.contains("For JavaScript/TypeScript files"));
		assertTrue(prompt.contains("For SQL files"));
	}

	@Test
	void pullRequestPromptWithRulesIncludesCustomRulePacks() {
		ReviewRulePackService reviewRulePackService = new ReviewRulePackService();
		CodeReviewService service = new CodeReviewService(
			new PlaceholderAiReviewClient(),
			new LanguageDetectionService(),
			reviewRulePackService
		);

		String prompt = service.buildPullRequestReviewPromptWithRules(
			new PullRequestReviewData(
				new PullRequestCoordinates("owner", "repo", 5),
				"https://github.com/owner/repo/pull/5",
				List.of(new PullRequestFile("src/main/java/App.java", "modified", 3, 1, "@@ java patch"))
			),
			List.of(
				new RulePack("Generic Review", List.of("Check security issues")),
				new RulePack("Java", List.of("Check null handling")),
				new RulePack("Spring Boot", List.of("Check DI boundaries"))
			)
		);

		assertTrue(prompt.contains("[Generic Review]"));
		assertTrue(prompt.contains("[Java]"));
		assertTrue(prompt.contains("[Spring Boot]"));
		assertTrue(prompt.contains("Check null handling"));
		assertTrue(prompt.contains("Organization rule packs:"));
	}
}
