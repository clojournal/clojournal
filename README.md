# eledger [![CircleCI](https://circleci.com/gh/pepijn/eledger.svg?style=svg&circle-token=27f5ba848e50cda482c8b46f5a6835ff571ba9f3)](https://circleci.com/gh/pepijn/eledger)

Ledger CLI wrapper that supports EDN input and output. Useful for those who want to benefit from both Ledger's power and EDN's flexibility. This library enables you to interact with ledger using EDN collections as input and output---no need to format journal files and parse CSV.

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
[{::eledger/date           "2019-06-01"
  ::eledger/transaction-id #uuid "960e3e3d-1d5d-45d9-aa92-50f7bfcd2efc"
  ::eledger/payee          "Mister Shawarma"
  ::eledger/postings       [{::eledger/account :expenses/food
                             ::eledger/amount  "R$ 20"}
                            {::eledger/account :assets/cash}]}
 {::eledger/date     #time/date "2019-07-01"
  ::eledger/payee    "Interactive Brokers"
  ::eledger/postings [{::eledger/account :assets/stocks
                       ::eledger/amount  "USD 1336"}
                      {::eledger/account :expenses/commissions
                       ::eledger/amount  "USD 1"}
                      {::eledger/account :assets/checking}]}]
```

### `command`

Either `::eledger/edn-register` for a register with EDN output or any string that will be used as Ledger CLI's command.

### `options`

TODO
