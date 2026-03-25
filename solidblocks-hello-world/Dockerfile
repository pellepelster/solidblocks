FROM gradle:9-jdk24-alpine AS build
WORKDIR /
COPY . .
RUN gradle installDist

FROM eclipse-temurin:24-jre-alpine
WORKDIR /
COPY --from=build /build/install/solidblocks-hello-world .
EXPOSE 8080
ENTRYPOINT ["./bin/solidblocks-hello-world"]
