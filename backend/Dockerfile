FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew build -x test

CMD ["java","-jar","build/libs/chat-0.0.1-SNAPSHOT.jar"]