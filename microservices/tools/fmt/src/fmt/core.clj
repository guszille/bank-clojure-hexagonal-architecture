(ns fmt.core
    (:require [fmt.format :as fmt]
              [rewrite-clj.parser :as p]
              [rewrite-clj.node :as n]
              [clojure.java.io :as io]
              [clojure.string :as str]
    )
    (:gen-class)
)

(def ^:private exts [".clj" ".cljs" ".cljc"])
(def ^:private skip-dirs ["target" ".shadow-cljs" ".cpcache" "node_modules"])

(defn- skip? [^String path]
    (some (fn [d] (str/includes? path (str "/" d "/"))) skip-dirs)
)

(defn- clojure-file? [^java.io.File f]
    (and (.isFile f)
         (some #(str/ends-with? (.getName f) %) exts)
         (not (skip? (.getPath f)))
    )
)

(defn- clojure-files [root]
    (filter clojure-file? (file-seq (io/file root)))
)

;; --- Safety guard ---------------------------------------------------------------------------------------------------------
;; The flattened sequence of every non-whitespace leaf (tokens, delimiters' contents, comments). Formatting only touches
;; whitespace, so this "skeleton" must be identical before and after -- otherwise the formatter changed the code and we
;; refuse to write the file.
(defn- gap? [node] (contains? #{:whitespace :newline :comma} (n/tag node)))

(defn- leaves [node]
    (if (n/inner? node)
        (mapcat leaves (remove gap? (n/children node)))
        [(n/string node)]
    )
)

(defn- skeleton [s]
    (mapcat leaves (remove gap? (n/children (p/parse-string-all s))))
)

(defn- inspect [^java.io.File f]
    (let [orig (slurp f)
          out (fmt/format-string orig)]
        (cond
            (not= (skeleton orig) (skeleton out)) {:status :unsafe :file f}
            (= orig out)                          {:status :ok :file f}
            :else                                 {:status :changed :file f :out out}
        )
    )
)

(defn -main [& args]
    (let [[mode & roots] args
          files (mapcat clojure-files roots)
          results (doall (map inspect files))
          unsafe (filter #(= :unsafe (:status %)) results)
          changed (filter #(= :changed (:status %)) results)]
        (doseq [u unsafe] (println "UNSAFE (skipped, would alter code):" (.getPath ^java.io.File (:file u))))
        (case mode
            "check"
            (do
                (doseq [c changed] (println "needs formatting:" (.getPath ^java.io.File (:file c))))
                (println (format "Checked %d files: %d need formatting, %d unsafe." (count files) (count changed) (count unsafe)))
                (System/exit (if (or (seq changed) (seq unsafe)) 1 0))
            )
            "fix"
            (do
                (doseq [c changed]
                    (spit (:file c) (:out c))
                    (println "formatted:" (.getPath ^java.io.File (:file c)))
                )
                (println (format "Fixed %d of %d files (%d unsafe, left unchanged)." (count changed) (count files) (count unsafe)))
                (System/exit (if (seq unsafe) 1 0))
            )
            (do
                (println "usage: clojure -M:run <fix|check> <root>...")
                (System/exit 2)
            )
        )
    )
)
