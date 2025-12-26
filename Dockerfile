# Use the official Eclipse Temurin JDK 25 image as the base
FROM eclipse-temurin:25-jdk

WORKDIR /app

# Copy Maven wrapper and pom.xml for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Find the JAR file and create a symlink
RUN ls -la target/*.jar
RUN mv target/*.jar target/app.jar

# Expose port
EXPOSE 8080

# Run the application with Java 25 (Virtual Threads enabled by default)
# Note: Virtual threads are enabled by default in Java 21+
ENTRYPOINT ["java", "-jar", "target/app.jar"]
