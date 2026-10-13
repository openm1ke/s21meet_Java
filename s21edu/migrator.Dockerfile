FROM liquibase/liquibase:4.33.0

WORKDIR /liquibase

COPY src/main/resources/ /liquibase/changelog/

USER liquibase
