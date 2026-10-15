dependencies {
    testImplementation(platform(rootProject.libs.boot.bom))
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("io.grpc:grpc-netty-shaded")
}

tasks.test {
    doFirst {
        require(fileTree("src/test").files.isNotEmpty()) { "Cross-service suite is pending its owning tasks" }
    }
}
