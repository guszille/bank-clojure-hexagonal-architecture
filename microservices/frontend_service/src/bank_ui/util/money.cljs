(ns bank-ui.util.money
    "Money crosses the wire as a BigDecimal-tagged string with a trailing 'M' (e.g. \"100.00M\").
   ClojureScript has no BigDecimal, so we keep money as a canonical string and never coerce it to a
   JS number -- these helpers only add/strip the suffix at the wire boundary."
    (:require [clojure.string :as str])
)

(defn strip
    "Remove the trailing 'M' tag: \"100.00M\" -> \"100.00\". Passes other strings through."
    [v]
    (when (some? v)
        (let [s (str v)]
            (if (str/ends-with? s "M") (subs s 0 (dec (count s))) s)
        )
    )
)

(defn ->wire
    "Encode a user-entered amount as the backend's 'M'-suffixed string, or nil if blank."
    [v]
    (let [s (str/trim (str v))]
        (when-not (str/blank? s)
            (if (str/ends-with? s "M") s (str s "M"))
        )
    )
)

(defn display
    "Render money for the UI: \"100.00M\" -> \"100.00\"."
    [v]
    (or (strip v) "")
)
