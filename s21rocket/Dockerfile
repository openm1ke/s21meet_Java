FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY build/libs/app.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java","-jar","/app/app.jar"]
