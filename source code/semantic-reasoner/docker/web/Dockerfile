FROM tomcat:8.0.51-jre8-alpine

COPY ./reasoner-api/target/reasoner-api.war /usr/local/tomcat/webapps/reasoner-api.war
EXPOSE 8080
ENV graphdb http://graph-db:7200
ENV bugPredictorServer  http://tosca-smell:8080

# Add docker-compose-wait tool -------------------
ENV WAIT_VERSION 2.7.3
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.7.3/wait /wait
RUN chmod +x /wait

CMD ["catalina.sh","run"]
