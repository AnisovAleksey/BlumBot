FROM amazoncorretto:8-alpine3.19-jre

COPY build/install/BlumBot BlumBot

WORKDIR BlumBot

CMD ["bin/BlumBot", "-a", "1"]%