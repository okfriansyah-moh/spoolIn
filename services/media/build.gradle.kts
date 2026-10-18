import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test

val bootstrapTests = sourceSets["integrationTest"]
bootstrapTests.resources.srcDir("src/integrationTest/kotlin/com/spoolin/media/boundary/bootstrap/fixtures")

val bootstrapRuntimeResources by tasks.registering(Sync::class) {
    from(sourceSets["main"].resources) { exclude("db/migration/**") }
    from(bootstrapTests.resources)
    into(layout.buildDirectory.dir("bootstrap-runtime-resources"))
}
val foundationTestRuntime =
    files(
        bootstrapTests.output.classesDirs,
        sourceSets["main"].output.classesDirs,
        configurations["integrationTestRuntimeClasspath"],
        bootstrapRuntimeResources,
    )

tasks.named<Test>("test") {
    testClassesDirs = sourceSets["test"].output.classesDirs + bootstrapTests.output.classesDirs
    classpath = files(sourceSets["test"].output.classesDirs, configurations["testRuntimeClasspath"], foundationTestRuntime)
    filter { includeTestsMatching("*RulesTest") }
}

tasks.named<Test>("integrationTest") {
    classpath = foundationTestRuntime
    dependsOn("integrationTestClasses")
    filter { includeTestsMatching("com.spoolin.media.boundary.bootstrap.*BoundaryTest") }
}

kover {
    currentProject { sources { includedSourceSets.add("main") } }
}

tasks.named("koverXmlReport") { dependsOn("integrationTest", "architectureTest") }

springBoot { mainClass.set("com.spoolin.media.ApplicationKt") }

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { archiveClassifier.set("boot") }

val receiptIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            bootstrapTests.output.classesDirs,
            sourceSets["main"].output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.media.operations.receipts.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("receipt.test.classpath", classpath.asPath)
}

tasks.named("koverXmlReport") { dependsOn(receiptIntegrationTest) }

val workIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["test"].output.classesDirs + bootstrapTests.output.classesDirs
    dependsOn("testClasses")
    classpath =
        files(
            sourceSets["test"].output.classesDirs,
            configurations["testRuntimeClasspath"],
            foundationTestRuntime,
        )
    filter { includeTestsMatching("com.spoolin.media.operations.work.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("work.test.classpath", classpath.asPath)
}

tasks.named("koverXmlReport") { dependsOn(workIntegrationTest) }

val artworkUnitTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["test"].output.classesDirs + bootstrapTests.output.classesDirs
    dependsOn("testClasses")
    classpath = files(sourceSets["test"].output.classesDirs, configurations["testRuntimeClasspath"], foundationTestRuntime)
    filter { includeTestsMatching("com.spoolin.media.features.*") }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(artworkUnitTest) }

val artworkIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            bootstrapTests.output.classesDirs,
            sourceSets["main"].output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.media.recovery.processing.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("artwork.test.classpath", classpath.asPath)
}

tasks.named("koverXmlReport") { dependsOn(artworkIntegrationTest) }

val receiptIntegrationThresholds by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    setSource(files("src/integrationTest/kotlin/com/spoolin/media/operations/receipts"))
    config.setFrom(rootProject.file(".ai/bundles/1.2.1/profiles/kotlin/detekt.yml"))
    buildUponDefaultConfig = false
    ignoreFailures = false
    reports {
        xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/receipt-integration-thresholds.xml"))
        txt.outputLocation.set(layout.buildDirectory.file("reports/detekt/receipt-integration-thresholds.txt"))
        html.outputLocation.set(layout.buildDirectory.file("reports/detekt/receipt-integration-thresholds.html"))
        sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/receipt-integration-thresholds.sarif"))
        md.outputLocation.set(layout.buildDirectory.file("reports/detekt/receipt-integration-thresholds.md"))
    }
}
