# clojournal [![CircleCI](https://circleci.com/gh/clojournal/clojournal.svg?style=svg&circle-token=27f5ba848e50cda482c8b46f5a6835ff571ba9f3)](https://circleci.com/gh/pepijn/eledger)

**WARNING: Project in alpha state, not really usable yet!**

Ledger CLI wrapper that supports EDN input and output. Useful for those who want to benefit from both Ledger's power and EDN's flexibility. This library enables you to interact with ledger using EDN collections as input and output—no need to format journal files (input) and parse CSV (output).

## Rationale

I love using ledger for all my financial tasks—for me it's the perfect balance between features (fully fledged double entry tool) and simplicity (text, CLI based). The only problem I kept facing is that it's quite challenging to get the data in and out of ledger's formats (basically [a journal file](https://www.ledger-cli.org/3.0/doc/ledger3.html#Example-Journal-File) for inputs and [CSV export](https://www.ledger-cli.org/3.0/doc/ledger3.html#index-csv-exporting) for output).

I've adopted the pattern of converting ledger formats from and to [EDN](https://github.com/edn-format/edn) to make my data portable. In the process, I found myself doing the same transformations over and over again. For that reason I've created this library, the place where I'll put all the tools necessary to make this workflow work.

## Installation

Make sure you have Ledger CLI installed and available on your `$PATH`.

macOS with Homebrew: `brew install ledger`

## Usage

Use the `api/eledger` function.

### `(com.clojournal.alpha.api/eledger [transactions command options])`

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

## References

- Ledger CLI website: https://www.ledger-cli.org
- Ledger CLI tutorial: https://p.epij.nl/ledger-cli/accounting/2018/08/23/real-world-ledger-part-1/
