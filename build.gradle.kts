plugins {
	java
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.eos"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-cache")

	// 이미지 처리
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("net.coobird:thumbnailator:0.4.20")

	// 데이터베이스
	implementation("mysql:mysql-connector-java:8.0.33")

	// JSON 처리
	implementation("com.fasterxml.jackson.core:jackson-databind")
	
	//캐싱
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.github.ben-manes.caffeine:caffeine")
	// 로깅
	implementation("org.slf4j:slf4j-api")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("software.amazon.awssdk:s3:2.25.61")
	implementation("software.amazon.awssdk:url-connection-client:2.25.61")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
