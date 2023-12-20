plugins {
  java
  id("org.springframework.boot") version "2.7.5"
  id("io.spring.dependency-management") version "1.1.4"

  // Code quality plugins
  checkstyle
  jacoco
  id("org.sonarqube") version "4.4.1.3373"
}

group = "uk.nhs.hee.tis.trainee"
version = "1.2.0"

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
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:2021.0.3")
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:2.4.4")
    mavenBom("com.azure.spring:spring-cloud-azure-dependencies:4.6.0")
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
  val sentryVersion = "6.32.0"
  implementation("io.sentry:sentry-spring-boot-starter:$sentryVersion")
  implementation("io.sentry:sentry-logback:$sentryVersion")

  // Amazon SQS
  implementation("io.awspring.cloud:spring-cloud-starter-aws")
  implementation("io.awspring.cloud:spring-cloud-starter-aws-messaging")

  implementation("com.azure.spring:spring-cloud-azure-starter")
  implementation("com.azure:azure-storage-file-datalake")

  implementation("commons-io:commons-io:2.15.0")

  val testContainersVersion = "1.19.1"
  testImplementation("org.springframework.cloud:spring-cloud-starter")
  testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
  testImplementation("org.testcontainers:testcontainers:${testContainersVersion}")
  testImplementation("org.testcontainers:junit-jupiter:${testContainersVersion}")
}

checkstyle {
  config = resources.text.fromArchiveEntry(configurations.checkstyle.get().first(), "google_checks.xml")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
    vendor.set(JvmVendorSpec.ADOPTIUM)
  }
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