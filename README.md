# AI Code Reviewer

A Spring Boot web application that reviews source code and GitHub Pull Requests using an AI model.

The app supports two review flows:

- Paste code directly into the UI and review it by language.
- Paste a GitHub Pull Request URL and review the changed files from the PR diff.

The backend builds structured review prompts, calls OpenAI when configured, and returns the review response to the frontend. If AI is disabled or no API key is configured, the app can fall back to a placeholder response.

## Live App

The app is running at [https://ai-code-reviewer-389730946099.asia-south1.run.app/](https://ai-code-reviewer-389730946099.asia-south1.run.app/).

## Features

- REST API for pasted-code review: `POST /api/review`
- REST API for GitHub Pull Request review: `POST /api/review-pr`
- OpenAI integration through the OpenAI Java SDK
- GitHub REST API integration for PR changed files
- Language-aware PR review prompts based on file names and extensions
- Friendly API error responses instead of stack traces
- Static frontend built with HTML, CSS, and JavaScript
- Two UI modes: code review and pull request review
- Tests for core service behavior, language detection, and endpoint validation

## Tech Stack

- Java 17
- Spring Boot 4.0.6
- Gradle
- OpenAI Java SDK `4.32.0`
- GitHub REST API
- HTML, CSS, JavaScript

## Project Structure

```text
src/main/java/com/soumenprogramming/ai_code_reviewer
├── AiCodeReviewerApplication.java
├── config
│   ├── AiReviewerConfig.java
│   ├── AiReviewerProperties.java
│   └── GitHubProperties.java
├── controller
│   └── CodeReviewController.java
├── dto
│   ├── ApiErrorResponse.java
│   ├── CodeReviewRequest.java
│   ├── CodeReviewResponse.java
│   ├── PullRequestCoordinates.java
│   ├── PullRequestFile.java
│   ├── PullRequestReviewData.java
│   └── PullRequestReviewRequest.java
├── exception
│   ├── ApiExceptionHandler.java
│   └── ApplicationException.java
└── service
    ├── AiReviewClient.java
    ├── CodeReviewService.java
    ├── GitHubPullRequestService.java
    ├── LanguageDetectionService.java
    ├── OpenAiReviewClient.java
    ├── PlaceholderAiReviewClient.java
    └── PullRequestReviewService.java
```

```text
src/main/resources
├── application.properties
└── static
    ├── index.html
    ├── app.js
    └── styles.css
```

## How It Works

### Pasted Code Review Flow

1. User enters a programming language and code in the frontend.
2. Frontend sends a request to `POST /api/review`.
3. `CodeReviewController` validates the request body.
4. `CodeReviewService` builds a structured AI prompt.
5. `AiReviewClient` sends the prompt to the configured AI implementation.
6. The backend returns the review as JSON.
7. The frontend displays the review output on the page.

### GitHub Pull Request Review Flow

1. User enters a GitHub PR URL in the frontend.
2. Frontend sends a request to `POST /api/review-pr`.
3. `CodeReviewController` validates the PR URL request body.
4. `GitHubPullRequestService` parses:
   - owner
   - repo
   - pull request number
5. The service calls GitHub REST API:

```text
GET https://api.github.com/repos/{owner}/{repo}/pulls/{pull_number}/files
```

6. For each changed file, the app collects:
   - filename
   - status
   - additions
   - deletions
   - patch
7. `LanguageDetectionService` detects languages from filenames and extensions.
8. `CodeReviewService` builds one language-aware PR review prompt.
9. OpenAI reviews the PR diff.
10. The frontend displays the review output.

## API Endpoints

### Review Pasted Code

```http
POST /api/review
Content-Type: application/json
```

Request:

```json
{
  "language": "Java",
  "code": "public class Demo { }"
}
```

Response:

```json
{
  "review": "Summary:\n..."
}
```

### Review GitHub Pull Request

```http
POST /api/review-pr
Content-Type: application/json
```

Request:

```json
{
  "prUrl": "https://github.com/owner/repo/pull/123"
}
```

Response:

```json
{
  "review": "Summary:\n..."
}
```

## Environment Variables

The application reads secrets and runtime config from environment variables.

| Variable | Required | Description |
| --- | --- | --- |
| `OPENAI_API_KEY` | Required for live AI review | OpenAI API key used by `OpenAiReviewClient` |
| `GITHUB_TOKEN` | Required for PR review | GitHub token used to fetch pull request files |
| `APP_AI_ENABLED` | Optional | Enables/disables live AI integration. Defaults to `true` |
| `APP_AI_MODEL` | Optional | OpenAI model name. Defaults to `gpt-5.4-mini` |
| `PORT` | Optional | Server port. Defaults to `8080` |

Example:

```bash
export OPENAI_API_KEY="your_openai_api_key"
export GITHUB_TOKEN="your_github_token"
export APP_AI_ENABLED=true
export APP_AI_MODEL="gpt-5.4-mini"
```

Do not hardcode API keys in `application.properties` or Java source files.

## Running Locally

Start the application:

```bash
./gradlew bootRun
```

Then open:

```text
http://localhost:8080
```

If port `8080` is already in use:

```bash
./gradlew bootRun --args='--server.port=8081'
```

Then open:

```text
http://localhost:8081
```

## Running Tests

```bash
./gradlew test
```

Current test coverage includes:

- Spring application context loading
- code review endpoint validation
- PR review endpoint validation
- GitHub PR URL parsing
- missing `GITHUB_TOKEN` error handling
- language detection
- language-aware PR prompt generation

## GitHub Token Setup

The PR review feature needs `GITHUB_TOKEN`.

For public repositories, a fine-grained token with read-only repository access is enough.

For private repositories, the token must have permission to read repository contents and pull requests.

The app does not post comments to GitHub. It only reads the PR diff and shows the AI review inside the application UI.

## OpenAI Integration

The app uses `OpenAiReviewClient` for live AI review.

When AI is enabled and `OPENAI_API_KEY` is configured:

```text
CodeReviewService -> AiReviewClient -> OpenAiReviewClient -> OpenAI Responses API
```

When AI is disabled or unavailable, the placeholder implementation can return a sample review:

```text
CodeReviewService -> AiReviewClient -> PlaceholderAiReviewClient
```

This design keeps the AI provider separate from the rest of the application logic.

## Language-Aware PR Review

`LanguageDetectionService` detects languages from changed file names and extensions.

Supported examples include:

- Java, Kotlin, Groovy, Gradle, Maven
- JavaScript, TypeScript, React, Vue, Svelte
- Python
- Go
- Rust
- C, C++, Objective-C
- C#
- PHP
- Ruby
- SQL
- Shell and PowerShell
- Terraform
- Dockerfile
- YAML, JSON, XML
- Markdown and common project config files

The PR prompt includes:

- a detected language summary
- detected language per file
- universal review checks
- language-specific review guidance only for languages present in the PR

This improves review relevance across different technology stacks.

## Error Handling

The app returns clean JSON error messages.

Example:

```json
{
  "message": "GITHUB_TOKEN is not configured. Set it before reviewing pull requests."
}
```

Handled cases include:

- missing request body
- missing language or code
- missing PR URL
- invalid GitHub PR URL
- missing `GITHUB_TOKEN`
- GitHub API failures
- invalid OpenAI API key
- OpenAI quota or rate-limit errors
- unexpected server errors

Stack traces are not exposed in browser responses.

## Frontend

The frontend is served from `src/main/resources/static`.

It contains:

- `index.html`: page structure
- `styles.css`: visual design
- `app.js`: mode switching and API calls

The UI has two modes:

- `Code`: paste code and select language
- `Pull Request`: enter GitHub PR URL

Both modes display output in the same review panel.

## Important Limitations

This app is an AI-assisted reviewer, not a replacement for human review.

AI review is not 100% accurate. It can:

- miss bugs
- misunderstand project context
- report false positives
- give generic advice
- miss cross-file behavior
- miss runtime or deployment-specific issues

The app improves usefulness by making prompts structured and language-aware, but final decisions should still be made by developers.

## Possible Future Improvements

- Add repository-wide context for better PR reviews
- Chunk very large PRs to avoid oversized prompts
- Add severity levels for findings
- Add inline GitHub comments after review approval
- Add authentication for the web app
- Store review history
- Add streaming AI responses
- Add model selection in the UI
- Add syntax highlighting in the code editor
- Add Dockerfile and deployment documentation

Contributors who want to work on any of the future improvements listed above are welcome to raise a pull request.

Himanshu Singh 
