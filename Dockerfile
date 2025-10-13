FROM openjdk:23
VOLUME /tmp
# copy the built jar (handles -SNAPSHOT naming)
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]