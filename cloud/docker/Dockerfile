# docker run -t . --build-arg SERVICE=product
FROM eclipse-temurin:17-jdk-alpine
ARG SERVICE=order
WORKDIR /server
ENV TZ="Asia/Shanghai"
ENTRYPOINT "./entrypoint.sh"

RUN apk update && apk add --no-cache bash

COPY ./entrypoint.sh /server/
COPY ./app/${SERVICE}.jar /server/application.jar

