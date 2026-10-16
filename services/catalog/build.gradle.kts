import org.gradle.api.tasks.testing.Test

val bootstrapTests = sourceSets["integrationTest"]
bootstrapTests.resources.srcDir("src/integrationTest/kotlin/com/spoolin/catalog/boundary/bootstrap/fixtures")

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

springBoot { mainClass.set("com.spoolin.catalog.ApplicationKt") }

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
    filter { includeTestsMatching("com.spoolin.catalog.operations.receipts.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
    systemProperty("receipt.test.classpath", classpath.asPath)
}

tasks.named("koverXmlReport") { dependsOn(receiptIntegrationTest) }
