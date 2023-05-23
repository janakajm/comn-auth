FROM fra.ocir.io/lolctech/fxapiuser/fusionx-base-image-jre:1.0.0
ARG GIT_COMMIT
ENV GIT_COMMIT=$GIT_COMMIT
ARG ARTIFACT
COPY ${ARTIFACT} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
