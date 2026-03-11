FROM liquibase/liquibase:4.31

WORKDIR /liquibase

COPY src/main/resources/ /liquibase/changelog/
