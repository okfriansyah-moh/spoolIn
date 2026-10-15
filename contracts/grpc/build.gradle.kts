import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.protobuf)
}

dependencyLocking {
    lockFile.set(layout.projectDirectory.file("gradle.lockfile"))
}

dependencies {
    implementation(rootProject.libs.protobuf)
    implementation(rootProject.libs.grpc.protobuf)
    implementation(rootProject.libs.grpc.stub)
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:${rootProject.libs.versions.protobuf.get()}" }
    plugins { id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:${rootProject.libs.versions.grpc.get()}" } }
    generateProtoTasks {
        all().configureEach {
            plugins { id("grpc") }
            generateDescriptorSet = true
            descriptorSetOptions.includeImports = true
            descriptorSetOptions.path =
                layout.buildDirectory
                    .file("descriptors/spoolin.pb")
                    .get()
                    .asFile.path
        }
    }
}

val generateTsContracts by tasks.registering(Exec::class) {
    dependsOn("extractIncludeProto")
    inputs.file("src/main/proto/spoolin/v1/spoolin.proto")
    outputs.dir(rootProject.layout.projectDirectory.dir("mcp/src/generated/spoolin"))
    outputs.dir(rootProject.layout.projectDirectory.dir("mcp/src/generated/google"))
    doFirst {
        val output =
            rootProject.layout.projectDirectory
                .dir("mcp/src/generated")
                .asFile
        output.mkdirs()
        commandLine(
            rootProject.configurations["protobufToolsLocator_protoc"].singleFile,
            "--plugin=protoc-gen-ts_proto=${rootProject.file("node_modules/.bin/protoc-gen-ts_proto")}",
            "--ts_proto_out=$output",
            "--ts_proto_opt=outputServices=grpc-js,env=node,esModuleInterop=true,forceLong=string,importSuffix=.js,useOptionals=messages,useDate=false,useJsonTimestamp=raw",
            "-I$projectDir/src/main/proto",
            "-I${layout.buildDirectory.dir("extracted-include-protos/main").get().asFile}",
            "$projectDir/src/main/proto/spoolin/v1/spoolin.proto",
        )
    }
}

tasks.named("generateProto") { finalizedBy(generateTsContracts) }

val coreOutput = project(":services:core").extensions.getByType<SourceSetContainer>()["main"].output
val consumerClasses = layout.buildDirectory.dir("consumer-classes")
val compileContractConsumer by tasks.registering(JavaCompile::class) {
    dependsOn(":services:core:classes", "classes")
    source(rootProject.file("tests/contracts/Consumer.java"))
    classpath = sourceSets["main"].runtimeClasspath + coreOutput
    destinationDirectory.set(consumerClasses)
}
tasks.register<JavaExec>("runContractConsumer") {
    dependsOn(compileContractConsumer)
    classpath = files(consumerClasses) + sourceSets["main"].runtimeClasspath + coreOutput
    mainClass.set("Consumer")
}
