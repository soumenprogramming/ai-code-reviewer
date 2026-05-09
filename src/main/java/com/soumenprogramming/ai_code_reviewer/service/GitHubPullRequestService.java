package com.soumenprogramming.ai_code_reviewer.service;

import com.soumenprogramming.ai_code_reviewer.config.GitHubProperties;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestCoordinates;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestFile;
import com.soumenprogramming.ai_code_reviewer.dto.PullRequestReviewData;
import com.soumenprogramming.ai_code_reviewer.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class GitHubPullRequestService {

	private static final Pattern PR_URL_PATTERN = Pattern.compile(
		"^https://github\\.com/([^/]+)/([^/]+)/pull/(\\d+)(?:/.*)?(?:\\?.*)?$"
	);

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final GitHubProperties gitHubProperties;

	public GitHubPullRequestService(HttpClient httpClient, ObjectMapper objectMapper, GitHubProperties gitHubProperties) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.gitHubProperties = gitHubProperties;
	}

	public PullRequestReviewData fetchPullRequestReviewData(String prUrl) {
		ensureTokenPresent();
		PullRequestCoordinates coordinates = parsePullRequestUrl(prUrl);
		List<PullRequestFile> files = fetchChangedFiles(coordinates);
		return new PullRequestReviewData(coordinates, prUrl, files);
	}

	PullRequestCoordinates parsePullRequestUrl(String prUrl) {
		if (!StringUtils.hasText(prUrl)) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, "Pull request URL is required.");
		}

		Matcher matcher = PR_URL_PATTERN.matcher(prUrl.trim());
		if (!matcher.matches()) {
			throw new ApplicationException(
				HttpStatus.BAD_REQUEST,
				"Invalid GitHub pull request URL. Use a URL like https://github.com/owner/repo/pull/123."
			);
		}

		return new PullRequestCoordinates(
			matcher.group(1),
			matcher.group(2),
			Long.parseLong(matcher.group(3))
		);
	}

	private List<PullRequestFile> fetchChangedFiles(PullRequestCoordinates coordinates) {
		List<PullRequestFile> files = new ArrayList<>();

		for (int page = 1; page <= 10; page++) {
			HttpRequest request = HttpRequest.newBuilder(buildFilesUri(coordinates, page))
				.header("Accept", "application/vnd.github+json")
				.header("Authorization", "Bearer " + gitHubProperties.getToken().trim())
				.header("X-GitHub-Api-Version", "2022-11-28")
				.GET()
				.build();

			HttpResponse<String> response = sendRequest(request);
			GitHubFileResponse[] pageFiles = parseFilesResponse(response.body());
			if (pageFiles.length == 0) {
				break;
			}

			for (GitHubFileResponse file : pageFiles) {
				files.add(new PullRequestFile(
					file.filename(),
					file.status(),
					file.additions(),
					file.deletions(),
					file.patch() == null ? "[Patch not available for this file]" : file.patch()
				));
			}

			if (pageFiles.length < 100) {
				break;
			}
		}

		if (files.isEmpty()) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, "No changed files were found for this pull request.");
		}

		return files;
	}

	private URI buildFilesUri(PullRequestCoordinates coordinates, int page) {
		String owner = URLEncoder.encode(coordinates.owner(), StandardCharsets.UTF_8);
		String repo = URLEncoder.encode(coordinates.repo(), StandardCharsets.UTF_8);
		String url = "%s/repos/%s/%s/pulls/%d/files?per_page=100&page=%d".formatted(
			trimTrailingSlash(gitHubProperties.getApiBaseUrl()),
			owner,
			repo,
			coordinates.pullNumber(),
			page
		);
		return URI.create(url);
	}

	private HttpResponse<String> sendRequest(HttpRequest request) {
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				return response;
			}

			throw buildGitHubException(response);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ApplicationException(
				HttpStatus.BAD_GATEWAY,
				"Failed to contact GitHub. Please try again."
			);
		} catch (IOException exception) {
			throw new ApplicationException(
				HttpStatus.BAD_GATEWAY,
				"Failed to contact GitHub. Please try again."
			);
		}
	}

	private GitHubFileResponse[] parseFilesResponse(String body) {
		try {
			return objectMapper.readValue(body, GitHubFileResponse[].class);
		} catch (JacksonException exception) {
			throw new ApplicationException(
				HttpStatus.BAD_GATEWAY,
				"GitHub returned an unreadable response."
			);
		}
	}

	private ApplicationException buildGitHubException(HttpResponse<String> response) {
		String fallbackMessage = switch (response.statusCode()) {
			case 401 -> "GitHub token is invalid. Update GITHUB_TOKEN and try again.";
			case 403 -> "GitHub access was denied. Check that GITHUB_TOKEN can read this repository.";
			case 404 -> "Pull request not found, or your GitHub token cannot access it.";
			default -> "GitHub request failed. Please try again later.";
		};

		String message = extractGitHubMessage(response.body(), fallbackMessage);
		HttpStatus status = response.statusCode() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
		if (response.statusCode() == 401 || response.statusCode() == 403) {
			status = HttpStatus.UNAUTHORIZED;
		}

		return new ApplicationException(status, message);
	}

	private String extractGitHubMessage(String responseBody, String fallbackMessage) {
		try {
			GitHubErrorResponse errorResponse = objectMapper.readValue(responseBody, GitHubErrorResponse.class);
			if (errorResponse.message() != null && !errorResponse.message().isBlank()) {
				return errorResponse.message();
			}
		} catch (JacksonException ignored) {
			// Ignore parsing errors and return the fallback message.
		}

		return fallbackMessage;
	}

	private void ensureTokenPresent() {
		if (!StringUtils.hasText(gitHubProperties.getToken())) {
			throw new ApplicationException(
				HttpStatus.SERVICE_UNAVAILABLE,
				"GITHUB_TOKEN is not configured. Set it before reviewing pull requests."
			);
		}
	}

	private String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return "https://api.github.com";
		}
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	private record GitHubFileResponse(
		String filename,
		String status,
		int additions,
		int deletions,
		String patch
	) {
	}

	private record GitHubErrorResponse(String message) {
	}
}
