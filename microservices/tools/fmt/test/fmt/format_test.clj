(ns fmt.format-test
    (:require [clojure.test :refer [deftest is testing]]
              [fmt.format :as fmt]
              [rewrite-clj.parser :as p]
              [rewrite-clj.node :as n]
    )
)

;; --- reparse-equality helper (mirrors the CLI safety guard) ---------------------------------------------------------------
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

(deftest converts-idiomatic-to-project-style
    (testing "2-space collapsed input becomes 4-space exploded output"
        (is (= (str "(defn foo [x]\n"
                    "    (let [y (inc x)]\n"
                    "        (bar y)\n"
                    "    )\n"
                    ")\n")
               (fmt/format-string "(defn foo [x]\n  (let [y (inc x)]\n    (bar y)))\n")
            )
        )
    )
)

(deftest ns-require-aligns-and-explodes
    (testing "require entries align under the first, the clause closes on its own line"
        (is (= (str "(ns foo.bar\n"
                    "    (:require [a.b :as b]\n"
                    "              [c.d :as d]\n"
                    "    )\n"
                    ")\n")
               (fmt/format-string "(ns foo.bar\n  (:require [a.b :as b]\n            [c.d :as d]))\n")
            )
        )
    )
)

(deftest vectors-close-attached-lists-explode
    (testing "a multi-line let: binding vector keeps ']' attached, the list ')' explodes"
        (is (= (str "(let [a 1\n"
                    "      b 2]\n"
                    "    a\n"
                    ")\n")
               (fmt/format-string "(let [a 1\n      b 2]\n  a)\n")
            )
        )
    )
)

(deftest hiccup-vectors-block-indent-and-explode
    (testing "a keyword-headed (hiccup) vector block-indents its body +4 and explodes its ']'"
        (is (= (str "[:section\n"
                    "    [:h1 \"Accounts\"]\n"
                    "    [:p \"Body\"]\n"
                    "]\n")
               (fmt/format-string "[:section\n [:h1 \"Accounts\"]\n [:p \"Body\"]]\n")
            )
        )
    )
    (testing "single-line hiccup is left intact"
        (is (= "[:th \"Number\"]\n" (fmt/format-string "[:th   \"Number\"]\n")))
    )
)

(deftest nested-lists-explode-each-level
    (is (= (str "(a\n"
                "    (b\n"
                "        (c)\n"
                "    )\n"
                ")\n")
           (fmt/format-string "(a\n  (b\n    (c)))\n")
        )
    )
)

(deftest threading-aligns-under-first-step
    (testing "-> steps align under the first step and the form closes at its opening column"
        (is (= (str "(let [q (-> a\n"
                    "            (b)\n"
                    "            (c)\n"
                    "        )]\n"
                    "    q\n"
                    ")\n")
               (fmt/format-string "(let [q (-> a\n            (b)\n            (c))]\n  q)\n")
            )
        )
    )
)

(deftest single-line-forms-are-untouched
    (is (= "(foo bar baz)\n" (fmt/format-string "(foo bar baz)\n")))
    (is (= "{:a 1 :b 2}\n" (fmt/format-string "{:a 1   :b 2}\n")))
)

(deftest is-idempotent
    (testing "formatting already-formatted output yields no further change"
        (doseq [src ["(defn foo [x]\n  (let [y (inc x)]\n    (bar y)))\n"
                     "(ns a (:require [b :as b]))\n"
                     "(defrecord R [x]\n  P\n  (m [this] (do (a) (b))))\n"
                     "[:section\n [:h1 \"x\"]\n [:tbody (for [a xs] [:tr [:td a]])]]\n"]]
            (let [once (fmt/format-string src)]
                (is (= once (fmt/format-string once)) (str "unstable for: " (pr-str src)))
            )
        )
    )
)

(deftest preserves-code-meaning
    (testing "formatting only changes whitespace -- the parsed skeleton is identical"
        (doseq [src ["(defn foo [x]\n  (let [y (inc x)] (bar y)))\n"
                     "(ns a\n  (:require [b :as b]\n            [c :as c]))\n"
                     "(case x\n  :a 1\n  :b 2\n  3)\n"]]
            (is (= (skeleton src) (skeleton (fmt/format-string src))))
        )
    )
)
