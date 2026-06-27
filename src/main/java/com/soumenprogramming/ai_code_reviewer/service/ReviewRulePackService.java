package com.soumenprogramming.ai_code_reviewer.service;

import com.soumenprogramming.ai_code_reviewer.dto.RulePack;
import com.soumenprogramming.ai_code_reviewer.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewRulePackService {

	public List<RulePack> validateRulePacks(List<RulePack> rulePacks) {
		if (rulePacks == null || rulePacks.isEmpty()) {
			throw new ApplicationException(HttpStatus.BAD_REQUEST, "At least one rule pack is required.");
		}

		List<RulePack> validated = new ArrayList<>();
		for (RulePack rulePack : rulePacks) {
			if (rulePack == null) {
				throw new ApplicationException(HttpStatus.BAD_REQUEST, "Rule pack entries cannot be null.");
			}

			if (rulePack.name() == null || rulePack.name().isBlank()) {
				throw new ApplicationException(HttpStatus.BAD_REQUEST, "Each rule pack must have a name.");
			}

			if (rulePack.rules() == null || rulePack.rules().isEmpty()) {
				throw new ApplicationException(
					HttpStatus.BAD_REQUEST,
					"Rule pack '" + rulePack.name().trim() + "' must include at least one rule."
				);
			}

			List<String> cleanedRules = new ArrayList<>();
			for (String rule : rulePack.rules()) {
				if (rule != null && !rule.isBlank()) {
					cleanedRules.add(rule.trim());
				}
			}

			if (cleanedRules.isEmpty()) {
				throw new ApplicationException(
					HttpStatus.BAD_REQUEST,
					"Rule pack '" + rulePack.name().trim() + "' must include at least one non-blank rule."
				);
			}

			validated.add(new RulePack(rulePack.name().trim(), List.copyOf(cleanedRules)));
		}

		return List.copyOf(validated);
	}

	public String buildRulesSection(List<RulePack> rulePacks) {
		StringBuilder section = new StringBuilder();

		for (RulePack rulePack : rulePacks) {
			section.append("\n[").append(rulePack.name()).append("]\n");
			for (String rule : rulePack.rules()) {
				section.append("- ").append(rule).append('\n');
			}
		}

		return section.toString().trim();
	}
}
