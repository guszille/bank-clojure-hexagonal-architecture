@REM docker build -t clojure-bank -f dockerfile.clj .
@REM docker run --rm -p 8080:8080 clojure-bank

docker compose down -v
docker compose --env-file .env up --build