plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)     // @Component 등 Spring 스테레오타입 클래스를 자동으로 open — 없으면 프록시가 안 걸린다
    alias(libs.plugins.kotlin.jpa)        // @Entity 클래스에 no-arg 생성자를 자동 추가 — 없으면 Hibernate 가 인스턴스화 못 한다
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")   // SPEC §10 — Prometheus 스크래핑

    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation(libs.mysql.connector.j)

    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // 카카오 로그인 자체 발급 토큰(JWT). Spring Security 는 안 쓴다 — 세션 기반 OAuth2 Client
    // 오토컨피그가 우리가 원하는 "완전 무상태" 방향과 안 맞아서, RestClient 로 카카오 토큰/사용자
    // 정보를 직접 호출하고 우리 JWT 만 발급하는 얇은 구현으로 간다.
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}

// 기본 단위 테스트와 분리된 실제 MySQL 검증. 전용 로컬 DB 이름·포트만 허용한다.
tasks.register<JavaExec>("authDatabaseProbe") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.jeongsan.server.user.AuthDatabaseProbeKt")
}
