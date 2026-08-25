# 런타임 전용 이미지. Gradle 빌드는 이미지 안에서 하지 않는다 —
# GitHub Actions 가 먼저 :server:bootJar 로 jar 를 만들고, 여기서는 그것만 담는다.
# (도커 안에서 Gradle 을 돌리면 캐시가 매번 날아가 빌드가 몇 배 느려진다)
#
# eclipse-temurin 은 멀티아키 이미지라 amd64/arm64 어느 쪽에서 빌드해도 맞는 층을 가져온다.
# 다만 **빌드하는 러너의 아키텍처가 곧 이미지의 아키텍처**다 — OCI 가 ARM 이면
# 워크플로도 ARM 러너에서 돌아야 한다(ADR-006).
FROM eclipse-temurin:21-jre

WORKDIR /app

# 루트로 돌리지 않는다. 컨테이너가 뚫려도 호스트 권한으로 번지지 않게.
RUN groupadd --system jeongsan && useradd --system --gid jeongsan jeongsan

COPY --chown=jeongsan:jeongsan server.jar /app/server.jar

USER jeongsan

EXPOSE 8080

# 컨테이너 메모리 상한(compose 의 limits)을 JVM 이 인식하게 한다.
# 이게 없으면 JVM 이 호스트 전체 메모리 기준으로 힙을 잡아 OOM 으로 죽는다(ADR-006 의 우려).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/server.jar"]
