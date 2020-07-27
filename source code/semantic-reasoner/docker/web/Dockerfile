FROM tomcat:7.0.103-jdk8-corretto

COPY ./reasoner-api/target/reasoner-api.war /usr/local/tomcat/webapps/reasoner-api.war
EXPOSE 8080
ENV environment docker

RUN mkdir -p /usr/local/tomcat/webapps/Ansibles

# Add docker-compose-wait tool -------------------
ENV WAIT_VERSION 2.7.3
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.7.3/wait /wait
RUN chmod +x /wait

CMD ["catalina.sh","run"]
