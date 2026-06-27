package com.soumenprogramming.ai_code_reviewer.service;

import com.soumenprogramming.ai_code_reviewer.dto.RulePack;
import com.soumenprogramming.ai_code_reviewer.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewRulePackServiceTests {

	private final ReviewRulePackService service = new ReviewRulePackService();

	@Test
	void validateRulePacksAcceptsCustomRulePacks() {
		List<RulePack> validated = service.validateRulePacks(List.of(
			new RulePack("Java Standards", List.of("Check null handling", "Check exception flow")),
			new RulePack("Spring Boot", List.of("Check DI boundaries"))
		));

		assertEquals(2, validated.size());
		assertEquals("Java Standards", validated.get(0).name());
		assertEquals(List.of("Check null handling", "Check exception flow"), validated.get(0).rules());
	}

	@Test
	void validateRulePacksRejectsEmptyRules() {
		ApplicationException exception = assertThrows(
			ApplicationException.class,
			() -> service.validateRulePacks(List.of(new RulePack("Java Standards", List.of())))
		);

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertTrue(exception.getMessage().contains("must include at least one rule"));
	}

	@Test
	void buildRulesSectionFormatsCustomRulePacks() {
		String section = service.buildRulesSection(List.of(
			new RulePack("Generic Review", List.of("Check security issues", "Check performance")),
			new RulePack("Company Policy", List.of("No hardcoded secrets"))
		));

		assertTrue(section.contains("[Generic Review]"));
		assertTrue(section.contains("- Check security issues"));
		assertTrue(section.contains("[Company Policy]"));
		assertTrue(section.contains("- No hardcoded secrets"));
	}
}
