FROM gradle:7.4.2-jdk11-alpine AS BUILD_IMAGE
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle clean build --no-daemon

FROM quay.io/keycloak/keycloak:20.0
COPY --from=BUILD_IMAGE /home/gradle/src/build/libs/Entertainment-Viet-keycloak-extension-*.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build
