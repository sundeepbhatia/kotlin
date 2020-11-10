import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(project(":compiler:fir:entrypoint"))
    testImplementation(project(":compiler:cli"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }
    testImplementation(project(":kotlin-reflect"))

    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(projectTests(":compiler:new-tests-infrastructure:new-test-generator"))

    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation(intellijDep()) {
        // This dependency is needed only for FileComparisonFailure
        includeJars("idea_rt", rootProject = rootProject)
        isTransitive = false
    }

    testImplementation(intellijDep()) {
        includeJars(
            "commons-lang3",
            "commons-io",
            rootProject = rootProject
        )
    }

    // This is needed only for using FileComparisonFailure, which relies on JUnit 3 classes
    testRuntimeOnly(commonDep("junit:junit"))
    testRuntimeOnly(intellijDep()) {
        includeJars("jna", rootProject = rootProject)
    }
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    jvmArgs!!.removeIf { it.contains("-Xmx") }
    maxHeapSize = "3g"

    useJUnitPlatform()
}

testsJar()
