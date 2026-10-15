import org.jlleitschuh.gradle.ktlint.KtlintExtension

configure<KtlintExtension> { additionalEditorconfig.put("max_line_length", "120") }

kover {
    currentProject { sources { includedSourceSets.add("main") } }
}
