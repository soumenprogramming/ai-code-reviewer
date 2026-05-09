package com.soumenprogramming.ai_code_reviewer.service;

import com.soumenprogramming.ai_code_reviewer.dto.PullRequestFile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageDetectionServiceTests {

	private final LanguageDetectionService service = new LanguageDetectionService();

	@Test
	void detectLanguageUsesFilenameAndExtension() {
		assertEquals("Java", service.detectLanguage("src/main/java/App.java"));
		assertEquals("TypeScript React", service.detectLanguage("frontend/src/App.tsx"));
		assertEquals("Dockerfile", service.detectLanguage("Dockerfile"));
		assertEquals("Python project config", service.detectLanguage("pyproject.toml"));
		assertEquals("Unknown", service.detectLanguage("README"));
	}

	@Test
	void detectLanguageCountsPreservesFirstSeenOrder() {
		Map<String, Long> counts = service.detectLanguageCounts(List.of(
			new PullRequestFile("src/App.java", "modified", 3, 1, "patch"),
			new PullRequestFile("src/main.ts", "modified", 8, 2, "patch"),
			new PullRequestFile("src/test/AppTest.java", "added", 12, 0, "patch")
		));

		assertEquals(List.of("Java", "TypeScript"), List.copyOf(counts.keySet()));
		assertEquals(2L, counts.get("Java"));
		assertEquals(1L, counts.get("TypeScript"));
	}
}
