FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
COPY abstracts /app/abstracts
COPY embeddings/glove_corpus.txt /app/embeddings/glove_corpus.txt
COPY embeddings/minilm_abstracts.txt /app/embeddings/minilm_abstracts.txt
RUN chown -R appuser:appuser /app
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
