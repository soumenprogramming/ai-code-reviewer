package com.soumenprogramming.ai_code_reviewer.service;

import com.soumenprogramming.ai_code_reviewer.dto.PullRequestFile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LanguageDetectionService {

	private static final String UNKNOWN = "Unknown";

	private static final Map<String, String> EXACT_FILENAMES = Map.ofEntries(
		Map.entry("dockerfile", "Dockerfile"),
		Map.entry("makefile", "Makefile"),
		Map.entry("jenkinsfile", "Jenkins pipeline"),
		Map.entry("pom.xml", "Maven XML"),
		Map.entry("build.gradle", "Gradle"),
		Map.entry("build.gradle.kts", "Gradle Kotlin DSL"),
		Map.entry("settings.gradle", "Gradle"),
		Map.entry("settings.gradle.kts", "Gradle Kotlin DSL"),
		Map.entry("package.json", "JSON / Node.js"),
		Map.entry("package-lock.json", "JSON / Node.js"),
		Map.entry("yarn.lock", "Yarn lockfile"),
		Map.entry("pnpm-lock.yaml", "YAML / Node.js"),
		Map.entry("tsconfig.json", "JSON / TypeScript"),
		Map.entry("requirements.txt", "Python dependencies"),
		Map.entry("pyproject.toml", "Python project config"),
		Map.entry("go.mod", "Go module"),
		Map.entry("go.sum", "Go module"),
		Map.entry("cargo.toml", "Rust manifest"),
		Map.entry("cargo.lock", "Rust manifest"),
		Map.entry("composer.json", "PHP Composer"),
		Map.entry("gemfile", "Ruby Bundler")
	);

	private static final Map<String, String> EXTENSIONS = Map.ofEntries(
		Map.entry("java", "Java"),
		Map.entry("kt", "Kotlin"),
		Map.entry("kts", "Kotlin Script"),
		Map.entry("groovy", "Groovy"),
		Map.entry("gradle", "Gradle"),
		Map.entry("js", "JavaScript"),
		Map.entry("mjs", "JavaScript"),
		Map.entry("cjs", "JavaScript"),
		Map.entry("jsx", "JavaScript JSX"),
		Map.entry("ts", "TypeScript"),
		Map.entry("tsx", "TypeScript React"),
		Map.entry("py", "Python"),
		Map.entry("rb", "Ruby"),
		Map.entry("php", "PHP"),
		Map.entry("go", "Go"),
		Map.entry("rs", "Rust"),
		Map.entry("cs", "C#"),
		Map.entry("cpp", "C++"),
		Map.entry("cc", "C++"),
		Map.entry("cxx", "C++"),
		Map.entry("c", "C"),
		Map.entry("h", "C/C++ header"),
		Map.entry("hpp", "C++ header"),
		Map.entry("swift", "Swift"),
		Map.entry("scala", "Scala"),
		Map.entry("sh", "Shell"),
		Map.entry("bash", "Shell"),
		Map.entry("zsh", "Shell"),
		Map.entry("ps1", "PowerShell"),
		Map.entry("sql", "SQL"),
		Map.entry("html", "HTML"),
		Map.entry("css", "CSS"),
		Map.entry("scss", "SCSS"),
		Map.entry("sass", "Sass"),
		Map.entry("less", "Less"),
		Map.entry("json", "JSON"),
		Map.entry("yaml", "YAML"),
		Map.entry("yml", "YAML"),
		Map.entry("xml", "XML"),
		Map.entry("md", "Markdown"),
		Map.entry("tf", "Terraform"),
		Map.entry("tfvars", "Terraform"),
		Map.entry("vue", "Vue"),
		Map.entry("svelte", "Svelte"),
		Map.entry("dart", "Dart"),
		Map.entry("r", "R"),
		Map.entry("lua", "Lua"),
		Map.entry("ex", "Elixir"),
		Map.entry("exs", "Elixir"),
		Map.entry("erl", "Erlang"),
		Map.entry("clj", "Clojure"),
		Map.entry("fs", "F#"),
		Map.entry("fsx", "F#"),
		Map.entry("pl", "Perl"),
		Map.entry("pm", "Perl"),
		Map.entry("m", "Objective-C"),
		Map.entry("mm", "Objective-C++"),
		Map.entry("sol", "Solidity")
	);

	public String detectLanguage(String filename) {
		if (!StringUtils.hasText(filename)) {
			return UNKNOWN;
		}

		String normalized = filename.replace('\\', '/').trim();
		String leafName = normalized.substring(normalized.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);

		String exactMatch = EXACT_FILENAMES.get(leafName);
		if (exactMatch != null) {
			return exactMatch;
		}

		if (leafName.endsWith(".d.ts")) {
			return "TypeScript declaration";
		}

		int dotIndex = leafName.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == leafName.length() - 1) {
			return UNKNOWN;
		}

		return EXTENSIONS.getOrDefault(leafName.substring(dotIndex + 1), UNKNOWN);
	}

	public Map<String, Long> detectLanguageCounts(List<PullRequestFile> files) {
		Map<String, Long> languageCounts = new LinkedHashMap<>();
		for (PullRequestFile file : files) {
			String language = detectLanguage(file.filename());
			languageCounts.merge(language, 1L, Long::sum);
		}
		return languageCounts;
	}
}
