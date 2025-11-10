FROM amazoncorretto:17
ARG RUN_AS_USER
ARG RUN_AS_GROUP
ENV USER=docker
ENV UID=$RUN_AS_USER
ENV GID=$RUN_AS_GROUP
RUN addgroup --gid "$GID" "$USER" \
   && adduser \
   --disabled-password \
   --gecos "" \
   --home "$(pwd)" \
   --ingroup "$USER" \
   --no-create-home \
   --uid "$UID" \
   "$USER"
EXPOSE 8080
WORKDIR /app
RUN chown -R docker:docker /app
COPY --chown=docker:docker ./target/biostudies.war /app
CMD java -Dhttp.proxyHost="$proxy_host" -Dhttp.proxyPort="$proxy_port" -Dhttps.proxyHost="$proxy_host" -Dhttps.proxyPort="$proxy_port" -Djava.io.tmpdir=/tmp -Dlog-path=logs -Dlog.level=debug -Dtomcat.hostname=`hostname` -jar biostudies.war
