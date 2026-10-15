project(":services:core") {
    extensions.configure<SourceSetContainer> {
        named("main") {
            java.srcDir(layout.buildDirectory.dir("generated/sources/graphql"))
            resources.srcDir(layout.buildDirectory.dir("generated/resources/contracts"))
        }
    }
    tasks.named("compileJava") { dependsOn(rootProject.tasks.named("generateGraphql")) }
    tasks.named("processResources") { dependsOn(rootProject.tasks.named("generateGraphql")) }
}
