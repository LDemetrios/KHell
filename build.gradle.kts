plugins {
    kotlin("jvm") version "1.9.21"
    `java-library`
    `maven-publish`
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "org.ldemetrios"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.21")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2")
    testImplementation("io.kotest:kotest-property-jvm:5.6.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("org.ldemetrios:common-utils:1.0-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-script-runtime:RELEASE")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.7.21")
    implementation("io.github.kscripting:kscript-annotations:1.5.0")
}

tasks.named("jar") {
    dependsOn("test")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}


publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
    this.repositories {
        mavenLocal()
    }
}

////////////////////////////////////

// TODO Create a plugin?

val Task.dependecies: Set<Task> get() = this.taskDependencies.getDependencies(this)

fun registerTasksGraph(name: String, filename: String, descr: String, doSimplify: Boolean) {
    tasks.register(name) {
        group = "custom"
        description = descr
        doLast {
            val dotContent = StringBuilder()
            val dot = File(rootProject.rootDir, filename)


            dotContent.append("digraph {\n")
            dotContent.append("  graph [labelloc=t,fontsize=30];\n")

            val edges = mutableListOf<Pair<Task, Task>>()

            if (doSimplify) {
                for (from in tasks) {
                    val longestPaths = mutableMapOf<Task, Int>()
                    longestPaths[from] = 0
                    var lastStep = listOf(from)
                    while (lastStep.isNotEmpty()){
                        val newLastStep = mutableListOf<Task>()
                        for (node in lastStep) {
                            for(dep in node.dependecies) {
                                newLastStep.add(dep)
                                longestPaths[dep] = longestPaths[node]!! + 1
                            }
                        }
                        lastStep = newLastStep
                    }
                    for (node in from.dependecies) {
                        if (longestPaths[node]!! <= 1) {
                            edges.add(node to from)
                        }
                    }
                }
            } else {
                tasks.forEach { task ->
                    task.taskDependencies.getDependencies(task).forEach { dep ->
                        edges.add(dep to task)
                    }
                }
            }

            edges.forEach { (from, to) ->
                dotContent.append("  \"${from.name}\" -> \"${to.name}\"")
                dotContent.append(" [style=dotted]")
                dotContent.append("\n")
            }

            dotContent.append("\n")
            dotContent.append("}\n")

            dot.createNewFile()
            dot.writeText(dotContent.toString())

            project.exec {
                commandLine("dot", "-Tpng", "-O", filename)
                workingDir(rootProject.rootDir)
            }
            dot.delete()
        }
    }
}

registerTasksGraph("tasksGraph", "project.dot", "graph tasks using gviz", false)
registerTasksGraph("tasksGraphClean", "project-clean.dot", "graph tasks using gviz without redundant edges", true)

