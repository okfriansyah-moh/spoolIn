import com.google.protobuf.gradle.id
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.artifacts.dsl.LockMode
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

buildscript {
    dependencies {
        classpath(enforcedPlatform("tools.jackson:jackson-bom:3.1.7"))
        classpath("org.apache.commons:commons-lang3:3.20.0")
        classpath("org.freemarker:freemarker:2.3.35")
    }
    dependencyLocking.lockAllConfigurations()
    dependencyLocking.lockMode.set(LockMode.STRICT)
    dependencyLocking.lockFile.set(file("gradle/locks/plugins/gradle.lockfile"))
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.protobuf)
}

allprojects {
    group = "com.spoolin"
    version = "0.0.0-foundation"
    dependencyLocking {
        lockAllConfigurations()
        lockMode.set(LockMode.STRICT)
        lockFile.set(
            rootProject.layout.projectDirectory.file("gradle/locks/${project.path.replace(':', '_').ifEmpty { "root" }}/gradle.lockfile"),
        )
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

allprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> { jvmToolchain(21) }
        dependencies {
            "ktlint"("ch.qos.logback:logback-classic:1.5.38")
            "implementation"(platform(rootProject.libs.boot.bom))
            "implementation"(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:${rootProject.libs.versions.kotlin.get()}"))
            "implementation"(enforcedPlatform("tools.jackson:jackson-bom:${rootProject.libs.versions.jackson.get()}"))
            constraints {
                listOf("tomcat-embed-core", "tomcat-embed-el", "tomcat-embed-websocket").forEach { artifact ->
                    add("implementation", "org.apache.tomcat.embed:$artifact:${rootProject.libs.versions.tomcat.get()}") {
                        version {
                            strictly(
                                rootProject.libs.versions.tomcat
                                    .get(),
                            )
                        }
                        because("Approved security patch to the Boot 4.1.1 dependency set")
                    }
                }
            }
            "implementation"("org.jetbrains.kotlin:kotlin-stdlib")
            "implementation"("org.jetbrains.kotlin:kotlin-reflect")
            "testImplementation"(platform(rootProject.libs.boot.bom))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }
        tasks.withType<Jar>().configureEach {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
        }
        extensions.configure<KtlintExtension> { version.set(rootProject.libs.versions.ktlint) }
        extensions.configure<DetektExtension> {
            config.setFrom(rootProject.file(".ai/bundles/1.3.0/profiles/kotlin/detekt.yml"))
            buildUponDefaultConfig = true
        }
    }
}

configure(subprojects.filter { it.path.startsWith(":services:") }) {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.springframework.boot")
    dependencies {
        "implementation"(platform(rootProject.libs.spring.grpc.bom))
        "implementation"(project(":contracts:grpc"))
        "implementation"(project(":libraries:backend-support"))
        "implementation"("org.springframework.boot:spring-boot-starter-webmvc")
        "implementation"("org.springframework.boot:spring-boot-starter-graphql")
        "implementation"("org.springframework.boot:spring-boot-starter-security")
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
        "implementation"("org.springframework.boot:spring-boot-starter-actuator")
        "implementation"("org.springframework.boot:spring-boot-starter-grpc-server")
        "implementation"("org.springframework.boot:spring-boot-starter-grpc-client")
        "implementation"("org.flywaydb:flyway-database-postgresql")
        "testImplementation"("com.tngtech.archunit:archunit-junit5:${rootProject.libs.versions.archunit.get()}")
        "testImplementation"("org.testcontainers:testcontainers-postgresql")
        "testImplementation"("org.testcontainers:testcontainers-junit-jupiter")
        "runtimeOnly"("org.postgresql:postgresql")
    }
    tasks.named<Jar>("jar") { enabled = true }
    val integrationTest = extensions.getByType<SourceSetContainer>().create("integrationTest")
    extensions.configure<KotlinJvmProjectExtension> {
        target.compilations.getByName("integrationTest").associateWith(target.compilations.getByName("main"))
    }
    integrationTest.compileClasspath += sourceSets["main"].output
    integrationTest.runtimeClasspath += sourceSets["main"].output
    configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations["testImplementation"])
    configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations["testRuntimeOnly"])
    tasks.register<Test>("architectureTest") {
        testClassesDirs = integrationTest.output.classesDirs
        classpath = integrationTest.runtimeClasspath + sourceSets["main"].output
        useJUnitPlatform()
        filter { includeTestsMatching("*ArchitectureTest") }
    }
    tasks.register<Test>("integrationTest") {
        testClassesDirs = integrationTest.output.classesDirs
        classpath = integrationTest.runtimeClasspath + sourceSets["main"].output
        useJUnitPlatform()
        doFirst {
            require(fileTree("src/integrationTest").files.isNotEmpty()) { "Owner integration suite is not implemented" }
        }
    }
}

sourceSets {
    main {
        java.srcDir("build-tools/verification-tests/jvm/java")
        proto.srcDir("build-tools/verification-tests/jvm/proto")
    }
    test { java.srcDir("build-tools/verification-tests/jvm/test") }
}
kotlin.sourceSets.main { kotlin.srcDir("build-tools/verification-tests/jvm/kotlin") }

dependencies {
    testImplementation("com.tngtech.archunit:archunit-junit5:${libs.versions.archunit.get()}")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation(libs.protobuf)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}" }
    plugins { id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}" } }
    generateProtoTasks { all().configureEach { plugins { id("grpc") } } }
}

val generatorLinuxTools by configurations.creating
dependencies {
    listOf("linux-aarch_64", "linux-x86_64").forEach { classifier ->
        generatorLinuxTools("com.google.protobuf:protoc:${libs.versions.protobuf.get()}:$classifier@exe")
        generatorLinuxTools("io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}:$classifier@exe")
    }
}

val generatorClasspath by configurations.creating
dependencies {
    generatorClasspath(platform(libs.boot.bom))
    generatorClasspath(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:${libs.versions.kotlin.get()}"))
    generatorClasspath(enforcedPlatform("tools.jackson:jackson-bom:${libs.versions.jackson.get()}"))
    generatorClasspath("com.graphql-java:graphql-java")
}

tasks.register("writeGeneratorClasspath") {
    val output = layout.buildDirectory.file("generator-classpath.txt")
    outputs.file(output)
    doLast { output.get().asFile.writeText(generatorClasspath.asPath) }
}

tasks.register<Exec>("generateGraphql") {
    dependsOn("writeGeneratorClasspath")
    commandLine("node", "build-tools/generate-graphql.mjs")
}

tasks.register("generateContracts") {
    dependsOn("generateGraphql", ":contracts:grpc:generateProto")
}

tasks.register<Exec>("generateTsFixture") {
    dependsOn("generateProto")
    doFirst {
        val output =
            layout.buildDirectory
                .dir("generated/ts-fixture")
                .get()
                .asFile
        output.mkdirs()
        commandLine(
            configurations["protobufToolsLocator_protoc"].singleFile,
            "--plugin=protoc-gen-ts_proto=$rootDir/node_modules/.bin/protoc-gen-ts_proto",
            "--ts_proto_out=$output",
            "--ts_proto_opt=outputServices=grpc-js,env=node,esModuleInterop=true",
            "-Ibuild-tools/verification-tests/jvm/proto",
            "build-tools/verification-tests/jvm/proto/fixture.proto",
        )
    }
}

tasks.register("resolveLocks") {
    description = "Resolve every resolvable project configuration into strict lockfiles."
    doLast {
        allprojects.forEach { project ->
            project.configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
        }
    }
}

tasks.wrapper {
    gradleVersion = "8.14.3"
    distributionType = Wrapper.DistributionType.BIN
}

tasks.named<Detekt>("detekt") {
    setSource(files("build-tools/verification-tests/jvm/kotlin"))
}

val contractBuild = file("build-tools/contracts.gradle.kts")
if (contractBuild.isFile) {
    apply(from = contractBuild)
}

configure(
    subprojects.filter {
        it.path in setOf(":services:core", ":services:catalog", ":services:media", ":libraries:backend-support")
    },
) {
    extensions.configure<KoverProjectExtension> {
        reports.total {
            additionalBinaryReports.add(rootProject.file("tests/integration/build/kover/bin-reports/test.ic"))
        }
    }
    tasks.named("koverXmlReport") { dependsOn(":tests:integration:test") }
}

// The opt-in variant measures only src/test, without changing the combined acceptance reports.
if (providers.gradleProperty("unitCoverage").orNull == "true") {
    val unitModules = setOf(":services:core", ":services:catalog", ":services:media", ":libraries:backend-support")
    configure(subprojects.filter { it.path in unitModules }) {
        val sources = extensions.getByType<SourceSetContainer>()
        var executedTests = 0L
        var failedTests = 0L
        val unitTest = tasks.register<Test>("unitTest") {
            description = "Run the complete src/test suite without integration source sets or filters."
            group = "verification"
            testClassesDirs = sources["test"].output.classesDirs
            classpath = sources["test"].runtimeClasspath
            dependsOn("testClasses")
            useJUnitPlatform()
            maxParallelForks = 1
            maxHeapSize = "768m"
            systemProperty("spoolin.repository-root", rootProject.projectDir.absolutePath)
            ignoreFailures = true
            outputs.upToDateWhen { false }
            addTestListener(
                object : TestListener {
                    override fun beforeSuite(suite: TestDescriptor) = Unit
                    override fun beforeTest(testDescriptor: TestDescriptor) = Unit
                    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) = Unit
                    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                        if (suite.parent == null) {
                            executedTests = result.testCount - result.skippedTestCount
                            failedTests = result.failedTestCount
                        }
                    }
                },
            )
        }
        tasks.register("verifyUnitTests") {
            group = "verification"
            description = "Fail on empty or failed unit suites after their coverage reports are generated."
            dependsOn(unitTest, "koverXmlReportUnit", "koverHtmlReportUnit")
            doLast {
                require(executedTests > 0) { "No unit tests executed in $path" }
                require(failedTests == 0L) { "$failedTests unit tests failed in $path" }
            }
        }
        extensions.configure<KoverProjectExtension> {
            currentProject {
                instrumentation {
                    disabledForTestTasks.set(provider { tasks.withType<Test>().names.filter { it != unitTest.name } })
                }
                createVariant("unit") { add("jvm") }
            }
            reports.variant("unit") {
                filters { excludes { packages("com.spoolin.core.boundary.graphql.generated") } }
                xml { xmlFile.set(layout.buildDirectory.file("reports/unit-coverage/report.xml")) }
                html { htmlDir.set(layout.buildDirectory.dir("reports/unit-coverage/html")) }
                verify {
                    rule("At least 80 percent unit line coverage") { minBound(80) }
                }
            }
        }
    }
}
