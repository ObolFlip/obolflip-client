import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.1"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
}

group = "org.ergoplatform.obolflip"
version = "0.5.0"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
	maven("https://jitpack.io")
	maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	// Mosaik
	val mosaikVersion = "2.0.0"
	implementation("com.github.MrStahlfelge.mosaik:common-model:$mosaikVersion")
	implementation("com.github.MrStahlfelge.mosaik:common-model-ktx:$mosaikVersion")
	implementation("com.github.MrStahlfelge.mosaik:serialization-jackson:$mosaikVersion")

	// ErgoPay
	implementation ("org.ergoplatform:ergo-appkit_2.12:9c13af82-SNAPSHOT")
	implementation ("com.github.MrStahlfelge:ergoplatform-jackson:4.0.10")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
