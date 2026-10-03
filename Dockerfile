# Erster Schritt: Anwendung bauen
# Maven 3.9 ist Pflicht: 3.6.3 läuft nicht zuverlässig auf einem JDK 21.
# Das JDK im Builder darf neuer sein als das Compile-Ziel — `maven.compiler.release` in der
# pom.xml hält die API-Oberfläche trotzdem auf Java 18.
FROM maven:3.9-eclipse-temurin-21 AS builder

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

# Die Regressionstests berechnen reale Referenz-Charts und benötigen bereits
# während des Maven-Builds die Swiss-Ephemeris-Dateien (u. a. Chiron).
COPY ephe/ ./ephe/

# Bauen der Anwendung
RUN mvn clean package

# Zweiter Schritt: Erstellen des Laufzeit-Containers
# Dasselbe Laufzeit-Image wie gAIster. Die `openjdk:*`-Images sind abgekündigt.
FROM eclipse-temurin:21-jre-jammy

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
