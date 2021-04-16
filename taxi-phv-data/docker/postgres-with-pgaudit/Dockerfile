FROM postgres:11
RUN apt-get update && apt-get install -y --no-install-recommends \
  postgresql-$PG_MAJOR-pgaudit

CMD  ["postgres", "-c", "shared_preload_libraries=pgaudit"]