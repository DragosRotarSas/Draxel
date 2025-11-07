plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime:1.17.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.formdev:flatlaf:3.5.4")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("org.example.Main")
}

tasks.test {
    useJUnitPlatform()
}