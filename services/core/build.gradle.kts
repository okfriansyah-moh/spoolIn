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
    filter { includeTestsMatching("*BoundaryTest") }
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
    currentProject { sources { includedSourceSets.add("main") } }
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
