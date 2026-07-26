# Erster Schritt: Anwendung bauen
FROM maven:3.6.3-jdk-11 AS builder

# Arbeitsverzeichnis setzen
WORKDIR /app

# Kopieren der pom.xml und der Swisseph JAR-Datei
COPY pom.xml .
COPY libs/swisseph-2.00.00-01.jar /tmp/

# Installieren der Swisseph JAR-Datei im lokalen Maven-Repository im Container
RUN mvn install:install-file -Dfile=/tmp/swisseph-2.00.00-01.jar \
    -DgroupId=swisseph -DartifactId=swisseph -Dversion=2.00.00-01 -Dpackaging=jar

# Kopieren des restlichen Quellcodes
COPY src ./src

# Bauen der Anwendung
RUN mvn clean package

# Zweiter Schritt: Erstellen des Laufzeit-Containers
FROM openjdk:11-jre-slim

# Arbeitsverzeichnis setzen
WORKDIR /app

# Kopieren der gebauten JAR-Datei aus dem vorherigen Schritt
COPY --from=builder /app/target/astroapi-0.0.1-SNAPSHOT.jar /app/astroapi.jar

# Kopieren der Ephemeriden-Dateien
COPY ephe/ /app/ephe/

# Exponieren des Ports
EXPOSE 8080

# Starten der Anwendung
ENTRYPOINT ["java", "-jar", "astroapi.jar"]
