import org.jlleitschuh.gradle.ktlint.KtlintExtension

configure<KtlintExtension> { additionalEditorconfig.put("max_line_length", "120") }

kover {
    currentProject { sources { includedSourceSets.add("main") } }
}

val receiptOwners = listOf("core", "catalog", "media")
kover {
    reports {
        total {
            receiptOwners.forEach { owner ->
                additionalBinaryReports.add(
                    rootProject.layout.projectDirectory
                        .file(
                            "services/$owner/build/kover/bin-reports/receiptIntegrationTest.ic",
                        ).asFile,
                )
            }
        }
    }
}
tasks.named("koverXmlReport") {
    dependsOn(receiptOwners.map { ":services:$it:receiptIntegrationTest" })
}

dependencies {
    implementation("io.grpc:grpc-api:${rootProject.libs.versions.grpc.get()}")
    implementation(rootProject.libs.protobuf)
    implementation("tools.jackson.core:jackson-databind:${rootProject.libs.versions.jackson.get()}")
    compileOnly("ch.qos.logback:logback-classic:1.5.38")
}
