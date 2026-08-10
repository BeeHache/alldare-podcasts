# Build stage
FROM eclipse-temurin:25-jdk AS build
RUN apt-get update && apt-get install -y maven
WORKDIR /app

# Copy parent and common dependencies to leverage cache
COPY alldare-parent-stateless ./alldare-parent-stateless/
RUN mvn -f alldare-parent-stateless/pom.xml install -N
COPY alldare-parent ./alldare-parent/
RUN mvn -f alldare-parent/pom.xml install -N

COPY alldare-common ./alldare-common/
RUN mvn -f alldare-common/pom.xml install -DskipTests

COPY alldare-podcasts ./alldare-podcasts/

WORKDIR /app/alldare-podcasts
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy the built JAR from the podcasts service target directory
COPY --from=build /app/alldare-podcasts/target/podcasts-0.0.1-SNAPSHOT.jar app.jar


EXPOSE 8089 9089
ENTRYPOINT ["java", "-jar", "app.jar"]
