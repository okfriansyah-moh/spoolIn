import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

val coreMain = sourceSets["main"]
val coreTests = sourceSets["test"]
val bootstrapTests = sourceSets["integrationTest"]
val productionJar = tasks.named<Jar>("jar")
bootstrapTests.resources.srcDir("src/integrationTest/kotlin/com/spoolin/core/boundary/bootstrap/fixtures")

val bootstrapRuntimeResources by tasks.registering(Sync::class) {
    from(coreMain.resources) { exclude("db/migration/**") }
    from(bootstrapTests.resources)
    into(layout.buildDirectory.dir("bootstrap-runtime-resources"))
}
val foundationTestRuntime =
    files(
        bootstrapTests.output.classesDirs,
        coreMain.output.classesDirs,
        configurations["integrationTestRuntimeClasspath"],
        bootstrapRuntimeResources,
    )

tasks.named<Test>("test") {
    systemProperty("spoolin.repository-root", rootProject.projectDir.absolutePath)
    testClassesDirs = coreTests.output.classesDirs + bootstrapTests.output.classesDirs
    classpath =
        files(coreTests.runtimeClasspath, bootstrapTests.output.classesDirs, configurations["integrationTestRuntimeClasspath"])
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
            coreMain.output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.core.recovery.crypto.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("crypto.test.classpath", classpath.asPath)
}

dependencies {
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
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

tasks.register<JavaExec>("keyRotationCli") {
    group = "application"
    description = "Execute Core KeyRotationPlan (ops/bin/rotate-keys.sh --execute)"
    classpath = coreMain.runtimeClasspath
    mainClass.set("com.spoolin.core.operations.crypto.KeyRotationCliKt")
}

val receiptIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            bootstrapTests.output.classesDirs,
            coreMain.output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.core.operations.receipts.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("receipt.test.classpath", classpath.asPath)
}

tasks.named("koverXmlReport") { dependsOn(receiptIntegrationTest) }

val workIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = coreTests.output.classesDirs + bootstrapTests.output.classesDirs
    dependsOn("testClasses", "integrationTestClasses")
    classpath =
        files(
            coreTests.output.classesDirs,
            configurations["testRuntimeClasspath"],
            bootstrapTests.output.classesDirs,
            coreMain.output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.core.operations.work.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("work.test.classpath", classpath.asPath)
}

tasks.named("koverXmlReport") { dependsOn(workIntegrationTest) }

val catalogViewUnitTest by tasks.registering(Test::class) {
    testClassesDirs = coreTests.output.classesDirs
    classpath =
        files(
            coreTests.output.classesDirs,
            coreMain.output,
            configurations["testRuntimeClasspath"],
        )
    filter {
        includeTestsMatching("com.spoolin.core.features.catalogview.read.*RulesTest")
    }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(catalogViewUnitTest) }

val managedCatalogUnitTest by tasks.registering(Test::class) {
    testClassesDirs = coreTests.output.classesDirs
    classpath =
        files(
            coreTests.output.classesDirs,
            coreMain.output,
            configurations["testRuntimeClasspath"],
        )
    filter {
        includeTestsMatching("com.spoolin.core.features.catalogview.movie.*RulesTest")
    }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(managedCatalogUnitTest) }

val artworkProxyUnitTest by tasks.registering(Test::class) {
    testClassesDirs = coreTests.output.classesDirs
    classpath =
        files(
            coreTests.output.classesDirs,
            coreMain.output,
            configurations["testRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.core.features.artworkproxy.*RulesTest") }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(artworkProxyUnitTest) }

val lifecycleProjectionUnitTest by tasks.registering(Test::class) {
    testClassesDirs = coreTests.output.classesDirs
    classpath =
        files(
            coreTests.output.classesDirs,
            coreMain.output,
            configurations["testRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.core.features.catalogview.lifecycle.*RulesTest") }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(lifecycleProjectionUnitTest) }

val managedPersonUnitTest by tasks.registering(Test::class) {
    testClassesDirs = coreTests.output.classesDirs
    classpath =
        files(
            coreTests.output.classesDirs,
            coreMain.output,
            configurations["testRuntimeClasspath"],
        )
    filter {
        includeTestsMatching("com.spoolin.core.features.catalogview.person.*RulesTest")
    }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(managedPersonUnitTest) }

val managedCreditUnitTest by tasks.registering(Test::class) {
    testClassesDirs = coreTests.output.classesDirs
    classpath =
        files(
            coreTests.output.classesDirs,
            coreMain.output,
            configurations["testRuntimeClasspath"],
        )
    filter {
        includeTestsMatching("com.spoolin.core.features.catalogview.credit.*RulesTest")
    }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

tasks.named("koverXmlReport") { dependsOn(managedCreditUnitTest) }

val accountIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            bootstrapTests.output.classesDirs,
            coreMain.output,
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
    classpath = files(bootstrapTests.output.classesDirs, coreMain.output, configurations["integrationTestRuntimeClasspath"])
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
            coreMain.output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.core.browser.AccountBrowserFixtureTest") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("account.browser.handoff", rootProject.file(".ai/state/account-browser-public.json").absolutePath)
    outputs.upToDateWhen { false }
}

val draftIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            bootstrapTests.output.classesDirs,
            coreMain.output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.core.recovery.drafts.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("draft.test.classpath", classpath.asPath)
}

tasks.named("koverXmlReport") { dependsOn(draftIntegrationTest) }

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
    config.setFrom(rootProject.file(".ai/bundles/1.2.1/profiles/kotlin/detekt.yml"))
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

val alertUnitTest by tasks.registering(Test::class) {
    testClassesDirs = coreTests.output.classesDirs
    classpath = coreTests.runtimeClasspath
    filter { includeTestsMatching("com.spoolin.core.features.alerts.*") }
    useJUnitPlatform()
    maxHeapSize = "512m"
}

val alertIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath =
        files(
            bootstrapTests.output.classesDirs,
            coreMain.output,
            configurations["integrationTestRuntimeClasspath"],
        )
    filter { includeTestsMatching("com.spoolin.core.operations.alerts.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
}
tasks.named("koverXmlReport") { dependsOn(alertUnitTest, alertIntegrationTest) }

val mcpGatewayUnitTest by tasks.registering(Test::class) {
    testClassesDirs = coreTests.output.classesDirs
    classpath = coreTests.runtimeClasspath
    filter { includeTestsMatching("com.spoolin.core.features.mcpgateway.*") }
    useJUnitPlatform()
    maxHeapSize = "512m"
}
tasks.named("koverXmlReport") { dependsOn(mcpGatewayUnitTest) }

val artworkProxyIntegrationTest by tasks.registering(Test::class) {
    testClassesDirs = bootstrapTests.output.classesDirs
    dependsOn("integrationTestClasses")
    classpath = files(bootstrapTests.output.classesDirs, coreMain.output, configurations["integrationTestRuntimeClasspath"])
    filter { includeTestsMatching("com.spoolin.core.features.artworkproxy.*BoundaryTest") }
    useJUnitPlatform()
    maxHeapSize = "768m"
}
tasks.named("koverXmlReport") { dependsOn(artworkProxyIntegrationTest) }

// Managed by the pinned Boot BOM; used only for servlet async boundary tests.
dependencies { testImplementation("org.springframework:spring-test") }

// Only the foundation boundary fixture replaces production Flyway resources.
// Keep the ordinary jar explicit for all other suites, including custom Test tasks.
tasks.withType<Test>().configureEach {
    if (name != "integrationTest") {
        dependsOn(productionJar)
        classpath = classpath.filter { it != bootstrapTests.output.resourcesDir } + files(productionJar)
    }
}
