#!/usr/bin/env python3

import sys
import time
import json
import urllib.error
import urllib.request

def request_json(method: str, url: str, payload: dict | None = None) -> tuple[int, dict]:
    headers = {"Content-Type": "application/json"}
    data = None

    if payload is not None:
        data = json.dumps(payload).encode("utf-8")

    request = urllib.request.Request(url=url, data=data, headers=headers, method=method)

    try:
        with urllib.request.urlopen(request) as response:
            raw = response.read().decode("utf-8")

            return response.status, json.loads(raw) if raw else {}

    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")

        try:
            parsed = json.loads(raw) if raw else {}

        except json.JSONDecodeError:
            parsed = {"error": raw}

        return e.code, parsed

def poll_loan_until_settled(loan_id: str, timeout_seconds: float = 10.0, interval_seconds: float = 0.25) -> dict:
    deadline = time.monotonic() + timeout_seconds

    while time.monotonic() < deadline:
        print(f"Polling loan {loan_id}...")

        _, body = request_json("GET", f"http://localhost:8080/api/exchange/loans/{loan_id}")

        if body.get("status") in ("approved", "denied"):
            return body

        time.sleep(interval_seconds)

    raise TimeoutError(f"Loan {loan_id} did not settle within {timeout_seconds}s")

def exec_requests_flow_1():
    # Create investor's account.
    investor_account = request_json("POST", "http://localhost:8080/api/ledger/accounts")

    print("Investor's account created!")
    print(investor_account[1])

    # Create issuer's account.
    issuer_account = request_json("POST", "http://localhost:8080/api/ledger/accounts")

    print("Issuer's account created!")
    print(issuer_account[1])

    # Create a deposit transaction to investor's account.
    payload = {
        "type": "deposit",
        "value": "1000.00M",
        "source-account-id": "",
        "destination-account-id": investor_account[1]["id"]
    }
    transaction = request_json("POST", "http://localhost:8080/api/ledger/transactions", payload)

    print("Transaction created!")
    print(transaction[1])

    # Create investor.
    payload = {
        "account-id": investor_account[1]["id"]
    }
    investor = request_json("POST", "http://localhost:8080/api/exchange/investors", payload)

    print("Investor created!")
    print(investor[1])

    # Create issuer.
    payload = {
        "account-id": issuer_account[1]["id"]
    }
    issuer = request_json("POST", "http://localhost:8080/api/exchange/issuers", payload)

    print("Issuer created!")
    print(issuer[1])

    # Create loan.
    payload = {
        "principal": "100.00M",
        "rate": "10.00M",
        "inception-date": "2025-01-01",
        "term": 6,
        "investor-id": investor[1]["id"],
        "issuer-id": issuer[1]["id"]
    }
    loan = request_json("POST", "http://localhost:8080/api/exchange/loans", payload)

    print("Loan created!")
    print(loan[1])

    # Poll the loan until its status leaves "created".
    loan_id = loan[1]["id"]
    final_loan = poll_loan_until_settled(loan_id)

    print("Settled loan!")
    print(final_loan)

    # Retrieve investor's account.
    investor_account_id = investor_account[1]["id"]
    updated_investor_account = request_json("GET", f"http://localhost:8080/api/ledger/accounts/{investor_account_id}")

    print("Updated investor's account!")
    print(updated_investor_account[1])

    # Retrieve issuer's account.
    issuer_account_id = issuer_account[1]["id"]
    updated_issuer_account = request_json("GET", f"http://localhost:8080/api/ledger/accounts/{issuer_account_id}")

    print("Updated issuer's account!")
    print(updated_issuer_account[1])

def main() -> int:
    exec_requests_flow_1()

    return 0

if __name__ == "__main__":
    sys.exit(main())
