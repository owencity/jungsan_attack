plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)     // @Component 등 Spring 스테레오타입 클래스를 자동으로 open — 없으면 프록시가 안 걸린다
    alias(libs.plugins.kotlin.jpa)        // @Entity 클래스에 no-arg 생성자를 자동 추가 — 없으면 Hibernate 가 인스턴스화 못 한다
    kotlin("kapt")                        // QueryDSL Q-클래스 생성용 어노테이션 프로세서.
                                           // 버전 카탈로그 alias 로 별도 버전을 주면 이미 classpath 에 있는
                                           // kotlin-jvm 플러그인 버전과 충돌한다 — kotlin(...) 헬퍼로 같은 버전을 따라가게 한다
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

kotlin {
    jvmToolchain(21)
}

dependencyManagement {
    imports {
        // Spring Cloud 2025.0.3 는 Spring Boot 3.5.x 라인용이다.
        // 2025.1.x 이상은 Spring Boot 4.x 를 요구한다(spring-cloud-gateway 5.x) — 섞으면 해석이 깨진다.
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.3")
    }
}

dependencies {
    implementation(project(":core"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")   // SPEC §10 — Prometheus 스크래핑

    implementation("org.liquibase:liquibase-core")
    implementation(libs.mysql.connector.j)

    // QueryDSL — OpenFeign 포크. 원본(com.querydsl)은 Jakarta EE 네임스페이스 지원이 느려
    // 이 프로젝트는 처음부터 포크를 쓰기로 했다(SPEC §10).
    // querydsl-jpa 는 classifier 없이 하나뿐이다(7.6 기준 실제 배포물 확인).
    // querydsl-apt 쪽에만 jakarta/jpa/hibernate/general classifier 가 나뉘어 있어
    // 어노테이션 프로세서만 jakarta 로 지정한다.
    implementation(libs.querydsl.jpa)
    kapt("${libs.querydsl.apt.get()}:jakarta")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // 카카오 로그인 자체 발급 토큰(JWT). Spring Security 는 안 쓴다 — 세션 기반 OAuth2 Client
    // 오토컨피그가 우리가 원하는 "완전 무상태" 방향과 안 맞아서, RestClient 로 카카오 토큰/사용자
    // 정보를 직접 호출하고 우리 JWT 만 발급하는 얇은 구현으로 간다.
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
