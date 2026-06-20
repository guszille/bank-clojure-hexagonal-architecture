(ns bank-ui.views.common)

(defn short-id
    "Abbreviate a UUID for table cells; the full value is kept in the cell's title attribute."
    [id]
    (let [s (str id)]
        (if (> (count s) 8) (str (subs s 0 8) "…") s)
    )
)
