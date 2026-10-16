import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test

val bootstrapTests = sourceSets["integrationTest"]
bootstrapTests.resources.srcDir("src/integrationTest/kotlin/com/spoolin/core/boundary/bootstrap/fixtures")

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
    classpath =
        files(sourceSets["test"].output.classesDirs, configurations["testRuntimeClasspath"], foundationTestRuntime)
    filter { includeTestsMatching("*RulesTest") }
}

tasks.named<Test>("integrationTest") {
    classpath = foundationTestRuntime
    dependsOn("integrationTestClasses")
    filter { includeTestsMatching("com.spoolin.core.boundary.bootstrap.*BoundaryTest") }
}

val cryptoIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            bootstrapTests.output.classesDirs,
            sourceSets["main"].output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.core.recovery.crypto.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("crypto.test.classpath", classpath.asPath)
}

dependencies {
    implementation(libs.bouncycastle)
    implementation(libs.icu4j)
}

kover {
    currentProject {
        sources { includedSourceSets.add("main") }
        instrumentation { disabledForTestTasks.add("accountBrowserFixture") }
    }
}

tasks.named("koverXmlReport") { dependsOn("integrationTest", "architectureTest", cryptoIntegrationTest) }

springBoot { mainClass.set("com.spoolin.core.ApplicationKt") }

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
    filter { includeTestsMatching("com.spoolin.core.operations.receipts.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("receipt.test.classpath", classpath.asPath)
}

tasks.named("koverXmlReport") { dependsOn(receiptIntegrationTest) }

val accountIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            bootstrapTests.output.classesDirs,
            sourceSets["main"].output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter {
        includeTestsMatching("com.spoolin.core.features.session.*")
        includeTestsMatching("com.spoolin.core.recovery.crypto.CredentialIntegrationTest")
    }
    useJUnitPlatform()
    maxHeapSize = "768m"
}

tasks.named("koverXmlReport") { dependsOn(accountIntegrationTest) }

val admissionIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath = files(bootstrapTests.output.classesDirs, sourceSets["main"].output, configurations["integrationTestRuntimeClasspath"])
    filter { includeTestsMatching("com.spoolin.core.boundary.admission.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
}

tasks.named("koverXmlReport") { dependsOn(admissionIntegrationTest) }

val buildAccountFrontend by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("npm", "run", "build", "--workspace", "frontend")
    inputs.dir(rootProject.file("frontend/src"))
    inputs.files(rootProject.file("frontend/package.json"), rootProject.file("package-lock.json"))
    inputs.dir(rootProject.file("build-tools/verification-tests"))
    outputs.dir(rootProject.file("frontend/build"))
}
val stageAccountFrontend by tasks.registering(Sync::class) {
    dependsOn(buildAccountFrontend)
    from(rootProject.file("frontend/build")) { into("static") }
    into(layout.buildDirectory.dir("frontend-runtime"))
}
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    dependsOn(stageAccountFrontend)
    from(stageAccountFrontend) { into("BOOT-INF/classes") }
}
val accountBrowserFixture by tasks.registering(Test::class) {
    dependsOn("integrationTestClasses", stageAccountFrontend)
    testClassesDirs = bootstrapTests.output.classesDirs
    classpath =
        files(
            stageAccountFrontend,
            bootstrapTests.output.classesDirs,
            sourceSets["main"].output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.core.browser.AccountBrowserFixtureTest") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("account.browser.handoff", rootProject.file(".ai/state/account-browser-public.json").absolutePath)
    outputs.upToDateWhen { false }
}

val accountIntegrationThresholds by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    setSource(
        files(
            "src/integrationTest/kotlin/com/spoolin/core/features/session",
            "src/integrationTest/kotlin/com/spoolin/core/operations/receipts",
            "src/integrationTest/kotlin/com/spoolin/core/boundary/admission",
            "src/integrationTest/kotlin/com/spoolin/core/browser",
            "src/integrationTest/kotlin/com/spoolin/core/recovery/crypto/NonceTlsFixture.kt",
        ),
    )
    config.setFrom(rootProject.file(".ai/bundles/1.1.0/profiles/kotlin/detekt.yml"))
    buildUponDefaultConfig = false
    ignoreFailures = false
    reports {
        xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/account-integration-thresholds.xml"))
        txt.outputLocation.set(layout.buildDirectory.file("reports/detekt/account-integration-thresholds.txt"))
        html.outputLocation.set(layout.buildDirectory.file("reports/detekt/account-integration-thresholds.html"))
        sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/account-integration-thresholds.sarif"))
        md.outputLocation.set(layout.buildDirectory.file("reports/detekt/account-integration-thresholds.md"))
    }
}
