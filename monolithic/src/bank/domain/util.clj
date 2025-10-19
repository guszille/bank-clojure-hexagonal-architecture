(ns bank.domain.util)

(defn bigdec? [value]
    (instance? java.math.BigDecimal value)
)