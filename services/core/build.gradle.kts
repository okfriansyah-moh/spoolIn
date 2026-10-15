import org.gradle.api.tasks.testing.Test

val bootstrapTests = sourceSets["integrationTest"]
bootstrapTests.resources.srcDir("src/integrationTest/kotlin/com/spoolin/core/boundary/bootstrap/fixtures")

tasks.named<Test>("test") {
    testClassesDirs = bootstrapTests.output.classesDirs
    classpath = bootstrapTests.runtimeClasspath
    filter { includeTestsMatching("*RulesTest") }
}

tasks.named<Test>("integrationTest") {
    filter { includeTestsMatching("*BoundaryTest") }
}

kover {
    currentProject { sources { includedSourceSets.add("main") } }
}

tasks.named("koverXmlReport") { dependsOn("integrationTest", "architectureTest") }

springBoot { mainClass.set("com.spoolin.core.ApplicationKt") }

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { archiveClassifier.set("boot") }
