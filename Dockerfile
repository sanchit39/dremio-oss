FROM prevalentai/debian:bullseye-11.8 as builder
RUN apt install -y wget apt-transport-https gpg unzip
RUN wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
RUN echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F'=' '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
RUN apt update
RUN apt install --yes temurin-11-jdk temurin-17-jdk
RUN apt-get install --yes git vim
RUN cd /opt && wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip && unzip apache-maven-3.9.6-bin.zip -d /opt/
ENV PATH = ${PATH}:/opt/apache-maven-3.9.6/bin
RUN mkdir /root/.m2
COPY ./toolchains.xml /root/.m2/toolchains.xml
COPY ./settings.xml /root/.m2/settings.xml
RUN update-alternatives --set java /usr/lib/jvm/temurin-11-jdk-amd64/bin/java && update-alternatives --set javac /usr/lib/jvm/temurin-11-jdk-amd64/bin/javac
RUN cd /opt && git clone https://github.com/sanchit39/dremio-oss.git dremio
WORKDIR /opt/dremio
RUN ./mvnw install -DskipTests -Ddremio.oss-only=true
 
RUN sleep 60
 
FROM prevalentai/debian:bullseye-11.8
 
# Install Java and other required dependencies
RUN apt-get update && \
    apt-get install -y wget apt-transport-https gpg unzip git vim && \
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null && \
    echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F'=' '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -y temurin-11-jdk
 
RUN update-alternatives --set java /usr/lib/jvm/temurin-11-jdk-amd64/bin/java && update-alternatives --set javac /usr/lib/jvm/temurin-11-jdk-amd64/bin/javac
 
WORKDIR /opt/dremio
 
RUN mkdir -p /var/lib/dremio \
&& mkdir -p /opt/dremio \
&& mkdir -p /var/run/dremio \
&& mkdir -p /var/log/dremio \
&& mkdir -p /opt/dremio/data \
&& groupadd --system dremio \
&& useradd --base-dir /var/lib/dremio --system --gid dremio dremio \
&& chown -R dremio:dremio /opt/dremio/data \
&& chown -R dremio:dremio /var/run/dremio \
&& chown -R dremio:dremio /var/log/dremio \
&& chown -R dremio:dremio /var/lib/dremio
 
COPY --from=builder /opt/dremio/distribution/server/target/dremio-oss-25.0.0-*/dremio-oss-25.0.0-* /opt/dremio/
 
EXPOSE 9047/tcp
EXPOSE 31010/tcp
EXPOSE 45678/tcp
 
USER dremio
WORKDIR /opt/dremio
ENV DREMIO_HOME /opt/dremio
ENV DREMIO_PID_DIR /var/run/dremio
ENV DREMIO_GC_LOGS_ENABLED="no"
ENV DREMIO_LOG_DIR="/var/log/dremio"
ENV SERVER_GC_OPTS="-Xlog:gc* -Xlog:::time,level,tags -XX:+IgnoreUnrecognizedVMOptions"
ENTRYPOINT ["bin/dremio", "start-fg"]