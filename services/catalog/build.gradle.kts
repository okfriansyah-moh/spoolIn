import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test

val bootstrapTests = sourceSets["integrationTest"]
bootstrapTests.resources.srcDir("src/integrationTest/kotlin/com/spoolin/catalog/boundary/bootstrap/fixtures")

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
    doFirst {
        classpath = classpath.filter { it != sourceSets["main"].output.resourcesDir }
    }
    dependsOn("integrationTestClasses")
    filter { includeTestsMatching("com.spoolin.catalog.boundary.bootstrap.*BoundaryTest") }
}

kover {
    currentProject { sources { includedSourceSets.add("main") } }
}

tasks.named("koverXmlReport") { dependsOn("integrationTest", "architectureTest") }

springBoot { mainClass.set("com.spoolin.catalog.ApplicationKt") }

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { archiveClassifier.set("boot") }

tasks.register<JavaExec>("seedCli") {
    group = "application"
    description = "Load manifest titles via CatalogSeedLoader (ops/bin/seed.sh --apply)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.spoolin.catalog.ApplicationKt")
}

val receiptIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            bootstrapTests.output.classesDirs,
            sourceSets["main"].output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.catalog.operations.receipts.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("receipt.test.classpath", classpath.asPath)
}

tasks.named("koverXmlReport") { dependsOn(receiptIntegrationTest) }

val workIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["test"].output.classesDirs + bootstrapTests.output.classesDirs
    dependsOn("testClasses", "integrationTestClasses")
    classpath =
        files(
            sourceSets["test"].output.classesDirs,
            configurations["testRuntimeClasspath"],
            bootstrapTests.output.classesDirs,
            sourceSets["main"].output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.catalog.operations.work.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("work.test.classpath", classpath.asPath)
}

tasks.named("koverXmlReport") { dependsOn(workIntegrationTest) }

val catalogReadUnitTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath =
        files(
            sourceSets["test"].output.classesDirs,
            sourceSets["main"].output,
            configurations["testRuntimeClasspath"],
        )
    filter {
        includeTestsMatching("com.spoolin.catalog.features.search.*RulesTest")
        includeTestsMatching("com.spoolin.catalog.features.taxonomy.read.*RulesTest")
        includeTestsMatching("com.spoolin.catalog.features.person.read.*RulesTest")
        includeTestsMatching("com.spoolin.catalog.features.provenance.*RulesTest")
        includeTestsMatching("com.spoolin.catalog.seed.persistence.*RulesTest")
    }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(catalogReadUnitTest) }

val catalogMovieWriteUnitTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath =
        files(
            sourceSets["test"].output.classesDirs,
            sourceSets["main"].output,
            configurations["testRuntimeClasspath"],
        )
    filter {
        includeTestsMatching("com.spoolin.catalog.features.movie.*RulesTest")
        includeTestsMatching("com.spoolin.catalog.features.taxonomy.write.*RulesTest")
        includeTestsMatching("com.spoolin.catalog.operations.audit.*RulesTest")
    }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(catalogMovieWriteUnitTest) }

val catalogPersonWriteUnitTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath =
        files(
            sourceSets["test"].output.classesDirs,
            sourceSets["main"].output,
            configurations["testRuntimeClasspath"],
        )
    filter {
        includeTestsMatching("com.spoolin.catalog.features.person.write.*RulesTest")
    }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(catalogPersonWriteUnitTest) }

val catalogArtworkUnitTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath =
        files(
            sourceSets["test"].output.classesDirs,
            sourceSets["main"].output,
            configurations["testRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.catalog.features.artwork.*RulesTest") }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(catalogArtworkUnitTest) }

val catalogLifecycleUnitTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath =
        files(
            sourceSets["test"].output.classesDirs,
            sourceSets["main"].output,
            configurations["testRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.catalog.features.lifecycle.*RulesTest") }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(catalogLifecycleUnitTest) }

val catalogCreditWriteUnitTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath =
        files(
            sourceSets["test"].output.classesDirs,
            sourceSets["main"].output,
            configurations["testRuntimeClasspath"],
        )
    filter {
        includeTestsMatching("com.spoolin.catalog.features.credit.*RulesTest")
        includeTestsMatching("com.spoolin.catalog.features.taxonomy.roles.*RulesTest")
    }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(catalogCreditWriteUnitTest) }

val receiptIntegrationThresholds by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    setSource(files("src/integrationTest/kotlin/com/spoolin/catalog/operations/receipts"))
    config.setFrom(rootProject.file(".ai/bundles/1.3.0/profiles/kotlin/detekt.yml"))
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

val catalogLifecycleIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            bootstrapTests.output.classesDirs,
            sourceSets["main"].output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.catalog.recovery.lifecycle.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
}

tasks.named("koverXmlReport") { dependsOn(catalogLifecycleIntegrationTest) }

dependencies {
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}

val catalogProposalIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            sourceSets["integrationTest"].output.classesDirs,
            sourceSets["main"].output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.catalog.features.proposal.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
}
tasks.named("koverXmlReport") { dependsOn(catalogProposalIntegrationTest) }
