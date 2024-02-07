plugins {
    id("java")
}

group = "com.poly"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.itextpdf:itextpdf:5.4.3")
}

tasks.test {
    useJUnitPlatform()
}