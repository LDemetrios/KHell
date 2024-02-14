group = "org.ldemetrios"
version = "1.0"

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.21")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2")
    testImplementation("io.kotest:kotest-property-jvm:5.6.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("org.ldemetrios:common-utils:1.0")
    implementation(project(":khell-processes"))
}
