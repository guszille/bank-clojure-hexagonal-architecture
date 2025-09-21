docker build -t clojure-bank -f dockerfile.clj .
docker run --rm -p 8080:8080 clojure-bank