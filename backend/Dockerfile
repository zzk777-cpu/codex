FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY src ./src
RUN mkdir -p out \
    && javac -encoding UTF-8 -d out $(find src/main/java -name "*.java") \
    && jar --create --file app.jar -C out .
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/app.jar", "com.example.docsys.StandaloneServer"]
