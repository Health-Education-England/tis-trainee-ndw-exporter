plugins {
  java
  id("org.springframework.boot") version "3.4.2"
  id("io.spring.dependency-management") version "1.1.4"

  // Code quality plugins
  checkstyle
  jacoco
  id("org.sonarqube") version "5.1.0.4882"
}

group = "uk.nhs.hee.tis.trainee"
version = "1.5.0"

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

repositories {
  mavenCentral()
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.1.1")
    mavenBom("com.azure.spring:spring-cloud-azure-dependencies:5.9.1")
  }
}

dependencies {
  // Spring Boot starters
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web")
  testImplementation("org.springframework.boot:spring-boot-starter-test")

  // Lombok
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")

  // MapStruct
  val mapstructVersion = "1.5.5.Final"
  implementation("org.mapstruct:mapstruct:${mapstructVersion}")
  annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")
  testAnnotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")

  // Sentry reporting
  val sentryVersion = "7.10.0"
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:$sentryVersion")
  implementation("io.sentry:sentry-logback:$sentryVersion")

  implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
  implementation("io.awspring.cloud:spring-cloud-aws-starter-sns")
  implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")

  implementation("com.azure.spring:spring-cloud-azure-starter")
  implementation("com.azure:azure-storage-file-datalake")

  val testContainersVersion = "1.19.7"
  testImplementation("org.springframework.cloud:spring-cloud-starter")
  testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
  testImplementation("org.testcontainers:testcontainers:${testContainersVersion}")
  testImplementation("org.testcontainers:junit-jupiter:${testContainersVersion}")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
    vendor.set(JvmVendorSpec.ADOPTIUM)
  }
}

checkstyle {
  config = resources.text.fromArchiveEntry(configurations.checkstyle.get().first(), "google_checks.xml")
}

sonarqube {
  properties {
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.login", System.getenv("SONAR_TOKEN"))
    property("sonar.organization", "health-education-england")
    property("sonar.projectKey", "Health-Education-England_tis-trainee-ndw-exporter")

    property("sonar.java.checkstyle.reportPaths",
      "build/reports/checkstyle/main.xml,build/reports/checkstyle/test.xml")
  }
}

tasks.jacocoTestReport {
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
  useJUnitPlatform()
}
