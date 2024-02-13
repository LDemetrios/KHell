plugins {
    kotlin("jvm") version "1.9.21"
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}


group = "org.ldemetrios"
version = "1.0-SNAPSHOT"

//dependencies {
//    implementation("org.jetbrains.kotlin:kotlin-script-runtime:RELEASE")
//    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.7.21")
//    implementation("io.github.kscripting:kscript-annotations:1.5.0")
//}

val Task.dependecies: Set<Task> get() = this.taskDependencies.getDependencies(this)

tasks.register("publish-all") {
    allprojects.flatMap{it.tasks.filter {it.name == "publish"}}.forEach{
        this.dependsOn(it)
    }
}

//////////////////////////////////////
//
//// TODO Create a plugin?
//
//
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
                    while (lastStep.isNotEmpty()) {
                        val newLastStep = mutableListOf<Task>()
                        for (node in lastStep) {
                            for (dep in node.dependecies) {
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


subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://repo.maven.apache.org/maven2/")
        }
    }

    kotlin {
        jvmToolchain(21)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
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
        publications.create<MavenPublication>(project.name) {
            artifactId = (project.name)
            from(components["java"])
        }
        repositories {
            mavenLocal()
        }
    }
}
