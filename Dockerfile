FROM openjdk:23
VOLUME /tmp
COPY target/order_service.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]