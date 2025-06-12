import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

group = "com.wavjaby"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.scijava.org/content/groups/public")
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("PocketAI-Backend")
    archiveVersion.set("$version")
    archiveClassifier.set("")
    manifest {
        attributes("Main-Class" to "com.wavjaby.Main")
    }
//    dependencies {
//        exclude("com/fasterxml/jackson/databind/")
//    }
    minimize()
}

dependencies {
    implementation("io.github.sashirestela:simple-openai:3.19.4")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    implementation("net.imagej:ij:1.54j")
    implementation("io.scif:scifio:0.46.0")
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

//    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}


tasks.test {
    useJUnitPlatform()
}