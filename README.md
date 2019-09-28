# eledger [![CircleCI](https://circleci.com/gh/pepijn/eledger.svg?style=svg&circle-token=27f5ba848e50cda482c8b46f5a6835ff571ba9f3)](https://circleci.com/gh/pepijn/eledger)
Ledger CLI wrapper that supports EDN input and output

## Installation

Make sure you have Ledger CLI installed and available on your `$PATH`.

macOS with Homebrew: `brew install ledger`

## Usage

Use the `api/eledger` function.

### `(nl.epij.eledger.api/eledger [transactions command options])`

It takes a collection of transactions, a command and options:

### `transactions`

Pass a collection like this:

```clojure
[#:nl.epij.eledger{:date "2019-06-01",
                   :transaction-id #uuid "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc",
                   :payee "Mister Shawarma",
                   :postings [#:nl.epij.eledger{:account :expenses/food, :amount "R$ 20"}
                              #:nl.epij.eledger{:account :assets/cash}]}
 #:nl.epij.eledger{:date #time/date "2019-07-01",
                   :payee "Interactive Brokers",
                   :postings [#:nl.epij.eledger{:account :assets/stocks, :amount "USD 1336"}
                              #:nl.epij.eledger{:account :expenses/commissions, :amount "USD 1"}
                              #:nl.epij.eledger{:account :assets/checking}]}]
```

### `command`

Either `::eledger/edn-register` for a register with EDN output or any string that will be used as Ledger CLI's command.

### `options`

TODO
