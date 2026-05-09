package com.soumenprogramming.ai_code_reviewer.service;

import com.soumenprogramming.ai_code_reviewer.dto.CodeReviewRequest;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestFile;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestReviewData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CodeReviewService {

	private final AiReviewClient aiReviewClient;
	private final LanguageDetectionService languageDetectionService;

	public CodeReviewService(AiReviewClient aiReviewClient, LanguageDetectionService languageDetectionService) {
		this.aiReviewClient = aiReviewClient;
		this.languageDetectionService = languageDetectionService;
	}

	public String reviewCode(CodeReviewRequest request) {
		String prompt = buildReviewPrompt(request.language(), request.code());
		return callAiModel(prompt);
	}

	public String reviewPullRequest(PullRequestReviewData reviewData) {
		String prompt = buildPullRequestReviewPrompt(reviewData);
		return callAiModel(prompt);
	}

	String buildReviewPrompt(String language, String code) {
		return """
			You are an expert AI code reviewer.
			Review the submitted %s code like a senior engineer performing a pull request review.
			Focus on:
			1. Correctness bugs and risky logic
			2. Readability and maintainability
			3. Performance concerns
			4. Security issues
			5. Testing gaps
			6. Concrete refactoring suggestions

			Response format:
			- Start with "Summary:"
			- Then a "Findings:" section with concise bullet points
			- End with "Suggested next steps:"
			- Be direct and practical
			- If the code is solid, say so and explain why

			Code:
			%s
			""".formatted(safeLanguage(language), safeCode(code));
	}

	String buildPullRequestReviewPrompt(PullRequestReviewData reviewData) {
		Map<String, Long> languageCounts = languageDetectionService.detectLanguageCounts(reviewData.files());
		StringBuilder prompt = new StringBuilder("""
			You are an expert AI code reviewer analyzing a GitHub pull request diff.
			Review the pull request like a senior engineer for the detected language stack.

			Universal review checks:
			1. Bugs and risky logic
			2. Security issues
			3. Performance problems
			4. Language and framework best practices
			5. Null pointer risks
			6. SQL injection risks
			7. Exception handling gaps
			8. Concrete suggested improvements

			Language-specific guidance:
			%s

			Response format:
			- Start with "Summary:"
			- Then a "Findings:" section with concise bullet points
			- End with "Suggested next steps:"
			- Reference files when possible
			- If no significant issue is found, say so clearly

			Pull request:
			- URL: %s
			- Repository: %s/%s
			- Pull number: %d
			- Changed files: %d
			- Detected languages: %s

			Diff details:
			""".formatted(
			buildLanguageSpecificGuidance(languageCounts),
			reviewData.prUrl(),
			reviewData.coordinates().owner(),
			reviewData.coordinates().repo(),
			reviewData.coordinates().pullNumber(),
			reviewData.files().size(),
			formatLanguageSummary(languageCounts)
		));

		appendFiles(prompt, reviewData.files());
		return prompt.toString();
	}

	String callAiModel(String prompt) {
		return aiReviewClient.review(prompt);
	}

	private void appendFiles(StringBuilder prompt, List<PullRequestFile> files) {
		for (PullRequestFile file : files) {
			prompt.append("\n---\n");
			prompt.append("Filename: ").append(file.filename()).append('\n');
			prompt.append("Detected language: ").append(languageDetectionService.detectLanguage(file.filename())).append('\n');
			prompt.append("Status: ").append(defaultValue(file.status(), "unknown")).append('\n');
			prompt.append("Additions: ").append(file.additions()).append('\n');
			prompt.append("Deletions: ").append(file.deletions()).append('\n');
			prompt.append("Patch:\n").append(safePatch(file.patch())).append('\n');
		}
	}

	private String buildLanguageSpecificGuidance(Map<String, Long> languageCounts) {
		StringBuilder guidance = new StringBuilder();
		guidance.append("- Apply best practices for each detected language and avoid language-specific advice for unrelated files.\n");

		if (containsAny(languageCounts, "Java", "Kotlin", "Kotlin Script", "Groovy", "Gradle", "Gradle Kotlin DSL", "Maven XML")) {
			guidance.append("- For JVM/Spring files, check dependency injection boundaries, transaction safety, validation, null handling, exception mapping, and Spring configuration.\n");
		}

		if (containsAny(languageCounts, "JavaScript", "JavaScript JSX", "TypeScript", "TypeScript React", "TypeScript declaration", "Vue", "Svelte", "JSON / Node.js", "YAML / Node.js", "Yarn lockfile")) {
			guidance.append("- For JavaScript/TypeScript files, check type safety, async error handling, XSS risks, dependency usage, and client/server boundary mistakes.\n");
		}

		if (containsAny(languageCounts, "Python", "Python dependencies", "Python project config")) {
			guidance.append("- For Python files, check exception flow, typing assumptions, resource handling, dependency safety, and ORM/query parameterization.\n");
		}

		if (containsAny(languageCounts, "Go", "Go module")) {
			guidance.append("- For Go files, check explicit error handling, context cancellation, goroutine leaks, nil pointers, and interface design.\n");
		}

		if (containsAny(languageCounts, "Rust", "Rust manifest")) {
			guidance.append("- For Rust files, check ownership/lifetime assumptions, error propagation, unsafe blocks, concurrency, and API ergonomics.\n");
		}

		if (containsAny(languageCounts, "C#")) {
			guidance.append("- For C# files, check nullability, async/await flow, disposal, LINQ performance, dependency injection, and exception boundaries.\n");
		}

		if (containsAny(languageCounts, "C", "C++", "C/C++ header", "C++ header", "Objective-C", "Objective-C++")) {
			guidance.append("- For C/C++/Objective-C files, check memory ownership, bounds safety, undefined behavior, concurrency, and resource cleanup.\n");
		}

		if (containsAny(languageCounts, "Ruby", "Ruby Bundler")) {
			guidance.append("- For Ruby files, check nil handling, exception flow, metaprogramming risk, dependency safety, and SQL/ORM parameterization.\n");
		}

		if (containsAny(languageCounts, "PHP", "PHP Composer")) {
			guidance.append("- For PHP files, check input validation, escaping, SQL/query parameterization, dependency safety, and error handling.\n");
		}

		if (containsAny(languageCounts, "Swift", "Dart")) {
			guidance.append("- For mobile/client files, check lifecycle handling, async state updates, nullability, threading, and user-data safety.\n");
		}

		if (containsAny(languageCounts, "SQL")) {
			guidance.append("- For SQL files, check injection exposure, migration safety, indexes, locking, data integrity, and rollback risk.\n");
		}

		if (containsAny(languageCounts, "Shell", "PowerShell")) {
			guidance.append("- For scripts, check quoting, command injection, error handling, portability, and secret exposure.\n");
		}

		if (containsAny(languageCounts, "Terraform", "YAML", "YAML / Node.js", "JSON", "JSON / Node.js", "XML", "Dockerfile", "Maven XML", "Gradle", "Gradle Kotlin DSL")) {
			guidance.append("- For config and infrastructure files, check secret leakage, least privilege, environment drift, insecure defaults, and deployment safety.\n");
		}

		return guidance.toString().trim();
	}

	private String formatLanguageSummary(Map<String, Long> languageCounts) {
		if (languageCounts.isEmpty()) {
			return "Unknown";
		}

		StringBuilder summary = new StringBuilder();
		for (Map.Entry<String, Long> entry : languageCounts.entrySet()) {
			if (!summary.isEmpty()) {
				summary.append(", ");
			}
			summary.append(entry.getKey()).append(" (").append(entry.getValue()).append(" file");
			if (entry.getValue() != 1) {
				summary.append('s');
			}
			summary.append(')');
		}
		return summary.toString();
	}

	private boolean containsAny(Map<String, Long> languageCounts, String... languages) {
		for (String language : languages) {
			if (languageCounts.containsKey(language)) {
				return true;
			}
		}
		return false;
	}

	private String safeLanguage(String language) {
		if (language == null || language.isBlank()) {
			return "source";
		}
		return language.trim();
	}

	private String safeCode(String code) {
		if (code == null || code.isBlank()) {
			return "[No code provided]";
		}
		return code.trim();
	}

	private String safePatch(String patch) {
		if (patch == null || patch.isBlank()) {
			return "[Patch not available for this file]";
		}
		return patch.trim();
	}

	private String defaultValue(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		return value.trim();
	}
}
