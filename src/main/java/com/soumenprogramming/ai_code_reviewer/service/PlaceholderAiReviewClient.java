package com.soumenprogramming.ai_code_reviewer.service;

public class PlaceholderAiReviewClient implements AiReviewClient {

	@Override
	public String review(String prompt) {
		return """
			Summary:
			The code is understandable but would benefit from stronger validation and clearer structure.

			Findings:
			- Add input validation and error handling around external or user-provided values.
			- Break large methods into smaller units if business logic starts growing.
			- Prefer descriptive naming for variables and methods so intent is obvious.
			- Consider edge cases such as null input, empty collections, and invalid state transitions.
			- Add unit tests around the main execution path and known failure cases.

			Next step:
			Configure OPENAI_API_KEY and set app.ai.enabled=true to use a live model-backed review.
			""";
	}
}
