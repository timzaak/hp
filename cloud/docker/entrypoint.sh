#!/usr/bin/env bash
java -Duser.timezone=GMT+08 \
  -jar /server/application.jar --spring.config.location=file:/server/config/
