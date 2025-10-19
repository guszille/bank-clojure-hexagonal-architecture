(ns bank.domain.util)

(defn bigdec? [value]
    (instance? java.math.BigDecimal value)
)

(defn isodate? [value]
    (try
        (java.time.LocalDate/parse value)
        true
        (catch java.time.format.DateTimeParseException e
            false
        )
    )
)