# ============================================================
# Imagen de producción de TechNovaLD
# ============================================================
# Etapa 1: compilar el JAR con el Maven Wrapper.
# Se usa JDK 17 para respetar <java.version>17</java.version>.
# ============================================================

FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

# Copiar el wrapper y el POM primero para aprovechar la caché.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# Copiar el código y empaquetar. El POM fija <finalName>app</finalName>.
COPY src src
RUN ./mvnw clean package -DskipTests

# ============================================================
# Etapa 2: runtime mínimo, sin Maven ni código fuente.
# ============================================================

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system technovald \
    && useradd --system \
       --gid technovald \
       --create-home \
       --shell /usr/sbin/nologin \
       technovald

WORKDIR /app

COPY --from=build /app/target/app.jar app.jar
RUN chown -R technovald:technovald /app

USER technovald

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
