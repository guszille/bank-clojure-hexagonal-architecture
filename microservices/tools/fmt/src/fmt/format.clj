(ns fmt.format
    ;; A small, opinionated Clojure(Script) formatter that reproduces this project's hand-written backend
    ;; style: 4-space block indentation and "exploded" closing parens (a multi-line LIST closes with ')' on
    ;; its own line, at the list's own column). It re-indents and normalises inner spacing but PRESERVES the
    ;; author's hard line breaks -- it never re-wraps long lines.
    (:require [rewrite-clj.parser :as p]
              [rewrite-clj.node :as n]
              [clojure.string :as str]
    )
)

;; Forms whose body is indented by +4 from the line the form opens on (vs. arguments aligning under the
;; first argument). Matched by symbol *name* (namespace stripped), so e.g. ports/with-tx and
;; jdbc/with-transaction both resolve. Modeled on cljfmt's default indent rules.
(def block-forms
    #{"def" "defn" "defn-" "defmacro" "fn" "let" "letfn" "loop" "binding" "when" "when-let" "when-not"
      "when-first" "when-some" "if" "if-let" "if-not" "if-some" "while" "do" "doseq" "dotimes" "for"
      "cond" "cond->" "cond->>" "condp" "case" "try" "catch" "finally" "with-open" "with-local-vars"
      "with-redefs" "with-tx" "with-transaction" "defrecord" "deftype" "reify" "proxy" "defprotocol"
      "deftest" "testing" "are" "future" "delay" "locking" "doto" "-main" "ns"
    }
)

;; Forms whose direct LIST children are method definitions -- those bodies are block-indented regardless of
;; the (arbitrary) method name in head position.
(def method-forms
    #{"defrecord" "deftype" "reify" "proxy" "defprotocol" "extend-type" "extend-protocol" "extend"}
)

;; Threading macros: their wrapped steps align under the first step (and close at the opening column), like
;; ordinary calls, rather than block-indenting.
(def threading-forms
    #{"->" "->>" "some->" "some->>" "as->"}
)

(def ^:private gap-tags #{:whitespace :newline :comma})

(defn- gap? [node] (contains? gap-tags (n/tag node)))
(defn- comment-node? [node] (= :comment (n/tag node)))

(defn- spaces [n] (apply str (repeat (max 0 n) \space)))

(defn- has-newline? [nodes]
    (boolean (some #(str/includes? (n/string %) "\n") nodes))
)

(defn- append!
    ;; Append s to the buffer and return the resulting column (handles strings containing newlines).
    [^StringBuilder sb ^String s col]
    (.append sb s)
    (let [i (.lastIndexOf s "\n")]
        (if (neg? i) (+ col (count s)) (- (count s) (inc i)))
    )
)

(defn- count-newlines [nodes]
    (reduce (fn [acc node] (+ acc (count (filter #(= \newline %) (n/string node))))) 0 nodes)
)

(def ^:private coll-delims
    {:list ["(" ")"] :vector ["[" "]"] :map ["{" "}"] :set ["#{" "}"] :fn ["#(" ")"]}
)

(defn- head-name [node]
    (let [head (first (remove gap? (n/children node)))]
        (when (and head (= :token (n/tag head)))
            (try (let [s (n/sexpr head)] (when (symbol? s) (name s))) (catch Exception _ nil))
        )
    )
)

(defn- keyword-head? [node]
    (let [head (first (remove gap? (n/children node)))]
        (boolean
            (and head (= :token (n/tag head))
                 (try (keyword? (n/sexpr head)) (catch Exception _ false))
            )
        )
    )
)

(declare emit-node)

(defn- emit-meta [^StringBuilder sb node col li]
    ;; ^meta target -- emit the prefix then recurse into the target so a multi-line target gets re-indented.
    (let [contents (remove gap? (n/children node))
          metaval (first contents)
          target (second contents)
          broke (has-newline? (filter gap? (n/children node)))
          col (append! sb "^" col)
          col (emit-node sb metaval col li false)]
        (if target
            (let [col (if broke (append! sb (str "\n" (spaces li)) col) (append! sb " " col))]
                (emit-node sb target col li false)
            )
            col
        )
    )
)

(defn- emit-coll [^StringBuilder sb node col base-li force-block?]
    (let [tag (n/tag node)
          [open close] (coll-delims tag)
          ;; Symbol-headed lists block-indent their bodies (+4 from the form's line) and close at the line
          ;; indent; keyword-headed lists (ns :require/:import) and threading macros align under the first
          ;; argument and close at the opening column.
          kind (cond
                   (#{:vector :map :set} tag)                            :seq
                   (= tag :fn)                                           :align
                   (and (= tag :list) force-block?)                      :block
                   (and (= tag :list) (keyword-head? node))             :align
                   (and (= tag :list) (contains? threading-forms (head-name node))) :align
                   (= tag :list)                                         :block
                   :else                                                :align
               )
          methods? (and (= tag :list) (contains? method-forms (head-name node)))
          opener-col col
          col (append! sb open col)]
        (loop [items (n/children node)
               col col
               cur-li base-li
               idx 0
               anchor nil          ;; align: col of first arg; seq: col of first element (when on opener line)
               any-break false
               prev-comment false
               pending []]
            (if (empty? items)
                ;; --- closing delimiter ---
                (if (= kind :seq)
                    (append! sb close col)                       ;; vectors/maps/sets close attached to last element
                    (let [close-col (if (= kind :block) base-li opener-col)]
                        (if (or any-break (has-newline? pending))    ;; lists: explode when multi-line or author put ')' on its own line
                            (append! sb close (append! sb (str "\n" (spaces close-col)) col))
                            (append! sb close col)
                        )
                    )
                )
                (let [k (first items)]
                    (if (gap? k)
                        (recur (rest items) col cur-li idx anchor any-break prev-comment (conj pending k))
                        ;; content (or comment) node
                        (let [broke (or prev-comment (has-newline? pending))
                              blank (>= (count-newlines pending) 2)
                              ci (case kind
                                     :block (+ base-li 4)
                                     :align (or anchor (+ base-li 4))
                                     :seq   (or anchor (+ base-li 4))
                                 )
                              [col cur-li] (cond
                                               (and (zero? idx) (not broke)) [col cur-li]   ;; head hugs the opener
                                               (zero? idx)  [(append! sb (str "\n" (when blank "\n") (spaces ci)) col) ci]  ;; author put first element on its own line
                                               prev-comment [(append! sb (str (when blank "\n") (spaces ci)) col) ci]
                                               broke        [(append! sb (str "\n" (when blank "\n") (spaces ci)) col) ci]
                                               :else        [(append! sb " " col) cur-li]
                                           )
                              anchor (cond
                                         anchor anchor
                                         (and (= kind :seq) (zero? idx) (not broke)) col
                                         (and (= kind :align) (= idx 1) (not broke)) col
                                         :else anchor
                                     )
                              child-force-block? (and methods? (= :list (n/tag k)))
                              col (if (comment-node? k)
                                      (append! sb (n/string k) col)
                                      (emit-node sb k col cur-li child-force-block?)
                                  )]
                            (recur (rest items) col cur-li (inc idx) anchor (or any-break broke) (comment-node? k) [])
                        )
                    )
                )
            )
        )
    )
)

(defn- emit-node [^StringBuilder sb node col li force-block?]
    (case (n/tag node)
        (:list :vector :map :set :fn) (emit-coll sb node col li force-block?)
        :meta (emit-meta sb node col li)
        (append! sb (n/string node) col)            ;; tokens, strings, reader macros, quotes, ... verbatim
    )
)

(defn- emit-forms [^StringBuilder sb node]
    ;; Top level: forms at column 0, at most one blank line between them.
    (loop [items (n/children node) col 0 first? true prev-comment false pending []]
        (if (empty? items)
            col
            (let [k (first items)]
                (if (gap? k)
                    (recur (rest items) col first? prev-comment (conj pending k))
                    (let [blank (>= (count-newlines pending) 2)
                          col (cond
                                  first? col
                                  prev-comment (if blank (append! sb "\n" col) col)
                                  :else (append! sb (if blank "\n\n" "\n") col)
                              )
                          col (if (comment-node? k)
                                  (append! sb (n/string k) col)
                                  (emit-node sb k col 0 false)
                              )]
                        (recur (rest items) col false (comment-node? k) [])
                    )
                )
            )
        )
    )
)

(defn format-string
    "Format a whole Clojure(Script) source string to the project style. Pure; deterministic."
    [s]
    (let [root (p/parse-string-all s)
          sb (StringBuilder.)]
        (emit-forms sb root)
        (str (str/trimr (str sb)) "\n")
    )
)
