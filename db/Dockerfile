# This runs a postgres docker container and prepopulates it with all tables/data
#   needed to make the website function.  Any .SQL files placed in 
#   /docker-entrypoint-initdb.d are ran at container startup
# The initial SQL commands can be found at /db/init.sql
FROM library/postgres
ENV POSTGRES_PASSWORD password
RUN mkdir -p /docker-entrypoint-initdb.d
COPY init.sql /docker-entrypoint-initdb.d/
