FROM ubuntu:latest
LABEL authors="its"

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY target/*.jar app.jar
#expose remis a 8080 au lieu de 8081
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]