FROM sonarqube:26.7.0.124771-community

# The plugin has been named Creedengo since the ecoCode rename. 2.3.0 states
# compatibility "up to SonarQube 26.7.0", which is why the base image is
# pinned to 26.7 rather than the latest Community Build.
ARG CREEDENGO_JAVA_VERSION=2.3.0
ADD https://github.com/green-code-initiative/creedengo-java/releases/download/${CREEDENGO_JAVA_VERSION}/creedengo-java-plugin-${CREEDENGO_JAVA_VERSION}.jar \
    /opt/sonarqube/extensions/plugins/

# Files fetched by ADD from a URL land root-owned with mode 600, unreadable
# for the sonarqube user the server runs as. Open them up and drop back to
# that user.
USER root
RUN chmod 0644 /opt/sonarqube/extensions/plugins/creedengo-java-plugin-*.jar
USER sonarqube
