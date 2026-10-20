FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package \
    && cp "$(find target -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)" target/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
