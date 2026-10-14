# Runtime image for the JAR that Jenkins has already built and tested (no Maven build inside Docker).
FROM eclipse-temurin:21.0.12.1_1-jre-noble

WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --chown=app:app target/event-sponsorship-tracker-*.jar app.jar
USER app

EXPOSE 8080
# JVM options can be passed at run time with -e JAVA_TOOL_OPTIONS="...".
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
