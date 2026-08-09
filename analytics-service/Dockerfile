FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN chmod +x ./mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
