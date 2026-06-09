FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# 配置 Maven 仓库：阿里云 + Maven Central + JitPack
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>\
<settings>\
  <mirrors>\
    <mirror><id>aliyun</id><mirrorOf>*,!jitpack.io,!repo1.maven.org</mirrorOf><name>Aliyun</name><url>https://maven.aliyun.com/repository/public</url></mirror>\
  </mirrors>\
  <profiles>\
    <profile><id>repos</id><activation><activeByDefault>true</activeByDefault></activation>\
    <repositories>\
      <repository><id>central</id><url>https://repo1.maven.org/maven2/</url></repository>\
      <repository><id>jitpack</id><url>https://jitpack.io</url></repository>\
    </repositories>\
    </profile>\
  </profiles>\
</settings>' > /root/.m2/settings.xml

COPY pom.xml .
RUN mvn dependency:resolve -DskipTests 2>&1 | tail -5 || true
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
