(ns bank.ports.repository)

(defprotocol AccountRepository
    (save [this account] "Saves an account.")
    (get-by-id [this id] "Gets an account by their ID."))