val ownerProjects = listOf(":services:core", ":services:catalog", ":services:media").map(project::project)
val coreProject = project(":services:core")
val coreSources = coreProject.extensions.getByType<SourceSetContainer>()

dependencies {
    testImplementation(platform(rootProject.libs.boot.bom))
    testImplementation(project(":contracts:grpc"))
    testImplementation(project(":libraries:backend-support"))
    ownerProjects.forEach { owner ->
        testImplementation(
            files(
                owner.extensions
                    .getByType<SourceSetContainer>()["main"]
                    .output.classesDirs,
            ),
        )
    }
    testImplementation(files(coreSources["integrationTest"].output.classesDirs))
    testImplementation(files(coreProject.configurations["integrationTestRuntimeClasspath"]))
    testRuntimeOnly(files(coreSources["main"].output.resourcesDir))
}

tasks.test {
    dependsOn(":services:core:integrationTestClasses", ":services:catalog:classes", ":services:media:classes")
    doFirst {
        require(fileTree("src/test").files.isNotEmpty()) { "Cross-service suite is pending its owning tasks" }
    }
}

val leasesTest by tasks.registering(Test::class) {
    dependsOn(":services:core:integrationTestClasses", ":services:catalog:classes", ":services:media:classes")
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("com.spoolin.integration.work.leases.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
}

val lifecycleIntegrationTest by tasks.registering(Test::class) {
    dependsOn(":services:core:integrationTestClasses", ":services:catalog:classes", ":services:media:classes")
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("com.spoolin.integration.lifecycle.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
}

val artworkIntegrationTest by tasks.registering(Test::class) {
    dependsOn(":services:core:integrationTestClasses", ":services:catalog:classes", ":services:media:classes")
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("com.spoolin.integration.artwork.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
}

val catalogReadsTest by tasks.registering(Test::class) {
    dependsOn(":services:core:integrationTestClasses", ":services:catalog:classes", ":services:media:classes")
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("com.spoolin.integration.catalog.reads.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
}

val logicalMetricsTest by tasks.registering(Test::class) {
    dependsOn(":services:core:integrationTestClasses", ":services:catalog:classes", ":services:media:classes")
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("com.spoolin.integration.observability.*") }
    useJUnitPlatform()
    maxHeapSize = "768m"
}
