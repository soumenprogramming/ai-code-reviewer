const form = document.getElementById("review-form");
const prForm = document.getElementById("pr-review-form");
const languageInput = document.getElementById("language");
const codeInput = document.getElementById("code");
const submitButton = document.getElementById("submit-button");
const prUrlInput = document.getElementById("pr-url");
const prSubmitButton = document.getElementById("pr-submit-button");
const statusElement = document.getElementById("status");
const outputElement = document.getElementById("review-output");
const codeTab = document.getElementById("code-tab");
const prTab = document.getElementById("pr-tab");
const codeMode = document.getElementById("code-mode");
const prMode = document.getElementById("pr-mode");

codeTab.addEventListener("click", () => setActiveMode("code"));
prTab.addEventListener("click", () => setActiveMode("pr"));

form.addEventListener("submit", async (event) => {
	event.preventDefault();

	const payload = {
		language: languageInput.value.trim(),
		code: codeInput.value.trim()
	};

	if (!payload.language || !payload.code) {
		statusElement.textContent = "Validation error";
		outputElement.textContent = "Language and code are required.";
		return;
	}

	setLoadingState(submitButton, true, "Reviewing...");
	outputElement.textContent = "Requesting review...";

	try {
		await submitReview("/api/review", payload);
	} catch (error) {
		statusElement.textContent = "Request failed";
		outputElement.textContent = error.message || "Unexpected error while requesting the review.";
	} finally {
		setLoadingState(submitButton, false, "Review");
	}
});

prForm.addEventListener("submit", async (event) => {
	event.preventDefault();

	const payload = {
		prUrl: prUrlInput.value.trim()
	};

	if (!payload.prUrl) {
		statusElement.textContent = "Validation error";
		outputElement.textContent = "Pull request URL is required.";
		return;
	}

	setLoadingState(prSubmitButton, true, "Reviewing PR...");
	outputElement.textContent = "Fetching pull request diff and requesting review...";

	try {
		await submitReview("/api/review-pr", payload);
	} catch (error) {
		statusElement.textContent = "Request failed";
		outputElement.textContent = error.message || "Unexpected error while requesting the pull request review.";
	} finally {
		setLoadingState(prSubmitButton, false, "Review PR");
	}
});

async function submitReview(url, payload) {
	const response = await fetch(url, {
		method: "POST",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(payload)
	});

	if (!response.ok) {
		throw new Error(await extractErrorMessage(response));
	}

	const result = await response.json();
	statusElement.textContent = "Completed";
	outputElement.textContent = result.review;
}

async function extractErrorMessage(response) {
	try {
		const errorBody = await response.json();
		if (errorBody.message) {
			return errorBody.message;
		}
	} catch (error) {
		return "Review request failed.";
	}

	return "Review request failed.";
}

function setLoadingState(button, isLoading, loadingText) {
	button.disabled = isLoading;
	button.textContent = isLoading ? loadingText : button === submitButton ? "Review" : "Review PR";
	statusElement.textContent = isLoading ? "Reviewing" : statusElement.textContent;
}

function setActiveMode(mode) {
	const isCodeMode = mode === "code";

	codeTab.classList.toggle("active", isCodeMode);
	prTab.classList.toggle("active", !isCodeMode);
	codeMode.classList.toggle("active", isCodeMode);
	prMode.classList.toggle("active", !isCodeMode);
}
