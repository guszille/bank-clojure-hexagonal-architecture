FROM clojure:openjdk-17
WORKDIR /app

COPY deps.edn /app/
COPY src /app/src

EXPOSE 8080

CMD ["clojure", "-M", "-m", "bank.core"]