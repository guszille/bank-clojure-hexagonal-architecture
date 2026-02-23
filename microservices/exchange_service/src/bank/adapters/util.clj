(ns bank.adapters.util)

(defn parse-bigdec-fields [m]
    (into {}
        (map (fn [[k v]] [k (cond
            (instance? java.math.BigDecimal v) v
            (and (string? v) (.endsWith v "M")) (bigdec (subs v 0 (dec (count v))))
            :else v
        )]))
        m
    )
)

(defn compose-bigdec-fields [m]
    (into {}
        (map (fn [[k v]] [k (cond
            (instance? java.math.BigDecimal v) (str v "M")
            :else v
        )]))
        m
    )
)

(defn parse-uuid-string [value]
    {:pre [(string? value)]}
    (if (not (clojure.string/blank? value)) (java.util.UUID/fromString value) nil)
)
