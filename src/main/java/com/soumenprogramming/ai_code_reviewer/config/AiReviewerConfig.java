package com.soumenprogramming.ai_code_reviewer.config;

import com.soumenprogramming.ai_code_reviewer.service.AiReviewClient;
import com.soumenprogramming.ai_code_reviewer.service.OpenAiReviewClient;
import com.soumenprogramming.ai_code_reviewer.service.PlaceholderAiReviewClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties({AiReviewerProperties.class, GitHubProperties.class})
public class AiReviewerConfig {

	@Bean
	AiReviewClient aiReviewClient(AiReviewerProperties properties) {
		if (properties.isEnabled() && StringUtils.hasText(properties.getApiKey())) {
			return new OpenAiReviewClient(properties);
		}

		return new PlaceholderAiReviewClient();
	}

	@Bean
	HttpClient httpClient() {
		return HttpClient.newHttpClient();
	}
}
