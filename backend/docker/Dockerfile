FROM eclipse-temurin:17-jdk-alpine

WORKDIR /server
ENV TZ="Asia/Shanghai"
ENTRYPOINT "./entrypoint.sh"

RUN apk update && apk add --no-cache bash

COPY ./entrypoint.sh /server/
COPY ./app /server/

