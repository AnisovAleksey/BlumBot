FROM amazoncorretto:17-alpine

COPY . build/

RUN cd build && \
    ./gradlew installDist && \
    mv build/install/BlumBot /BlumBot && \
    cd .. && \
    rm -rf build

WORKDIR /BlumBot

CMD ["bin/BlumBot", "-a", "1"]%