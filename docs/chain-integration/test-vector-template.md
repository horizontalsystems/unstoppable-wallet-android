# <Chain>: history test vectors

What these vectors check, in one or two sentences.

Addresses are third-party and live; add them as watch accounts. Chain
snapshot: <YYYY-MM-DD HH:MM UTC>. Transactions newer than the snapshot are not
part of the vector. Times are UTC; the app shows local time, so dates may
shift.

## Mechanics under test

| On the chain | What the wallet must show | Where in code |
|---|---|---|
| | | |

Criteria for every address:

1. No hash appears twice in the coin wallet, in "All", or under the
   incoming and outgoing filters.
2. Amounts are at the right scale.
3. Fake tokens with a real ticker never mix with the real one and are not
   shown as a normal incoming.

## Address 1 — <what it covers>

`<address>` · <N> transactions on chain · <M> expected in the <coin> wallet

| UTC | Hash | On chain | In <coin> wallet | Why |
|---|---|---|---|---|
| | | | | |

| Run | Platform | Build | Result |
|---|---|---|---|
| <date> | Android | <commit> | passed / failed: <what> / not run: <reason> |
| <date> | iOS | <commit> | passed / failed: <what> / not run: <reason> |

## Not verified

What the vectors do not cover and why, so nobody reads silence as a pass.
