# New chain integration

How a new blockchain gets into the app. The iOS team ports most chains from
this code, so a defect we miss here usually ships twice. The steps below exist
because review of the finished diff kept missing whole classes of bugs: special
values lost after decimals rescaling, retries that pay twice, untrusted node
data shown as fact, sessions bound to the wrong account.

Every step leaves a file in `docs/chain-integration/<chain>/`. The files are
the record a reviewer, the iOS port, and a later fix read from.

| Step | Output | Done when |
|---|---|---|
| 1. Design | `design.md` with the delta matrix | Every row has a decision and a reason |
| 2. Build | kit, MarketKit, `walletkit-chain-<chain>` | `ChainBehaviorParityTest` fixture updated |
| 3. Test vectors | `test-vectors.md` | Every vector has a recorded result or "not run" with a reason |
| 4. Review | `review.md` with the decision log | Every finding is accepted, rejected, or in tech debt |
| 5. Hand-off | links sent to the iOS developer | Paired-platform check done both ways |

## 1. Design and the delta matrix

Before writing code, pick the closest existing chain as the analog: Stellar
for account-based chains with trust lines, Tron or EVM for contract tokens,
Bitcoin for UTXO. Then fill in the delta matrix: one row for every place the
new chain cannot simply copy the analog.

```markdown
| # | Area | Analog does | New chain does | Why | Checklist | file:line |
|---|---|---|---|---|---|---|
| 1 | Pending tx | Tron: write after broadcast | write before submit | a lost submit response must not let the user pay again | TX-4 | |
| 2 | History amount | EVM: `value` | `delivered_amount` only, never `Amount` | partial payments deliver less than requested | HIST-3 | |
```

The "Why" column is the point. A row that says "same as Tron" needs no reason;
a row that differs needs one a reviewer can check. Go through every section of
[checklist.md](checklist.md) and add a row for each item that applies, even if
the answer is "same as analog".

## 2. Build

The usual `ChainPlugin` work. Two things are easy to skip:

- Update `app/src/test/resources/chain-behavior-parity.txt` so the chain's
  registry behavior is pinned.
- Kit unit tests need expected values from outside our code: the chain's
  official test vectors, a block explorer, or the reference SDK. A test that
  derives the expected address with the same code it tests proves nothing.

## 3. Test vectors

Unit tests do not show what the user sees in history. Before review, write
`test-vectors.md` from the [template](test-vector-template.md): real addresses
added as watch accounts, each transaction with the row the wallet must show
and why. [arc/test-vectors.md](arc/test-vectors.md) is the reference example.

Pick addresses that between them cover:

- plain incoming and outgoing native transfers
- token transfers, including any native-token interface or mirror event
- a fee-only transaction (approve, contract call with zero value)
- an unlimited approve (`2^256−1` or the chain's maximum)
- internal transactions or other indirect value movement
- dust below the spam threshold
- address poisoning: a fake token with a real ticker, a lookalike sender
- the chain's own trick, if it has one (XRP partial payment, memo-required
  destinations)

Record the result on each run. A vector that was not run is written as
"not run" with the reason, never left blank or marked passed.

## 4. Review

Review the chain branch with several independent reviewers, each with one
role, before it is merged. With Claude Code, run the `/chain-review` skill; by
hand, give each reviewer one role and the checklist.

| Role | Asks |
|---|---|
| Correctness | Does every value shown, signed, and stored match the chain's rules? |
| Security | How would someone take money or mislead the user from here? |
| Resilience | What happens on timeout, lost response, node lag, restart, retry? |
| Adversary | Which delta-matrix decision is wrong, and what is the cheaper alternative? |

A finding counts only with a file:line, a reachable path from user action or
network input, and a concrete failure: "with input X the app shows or signs Y,
the chain requires Z". "There is no test for this" is not a finding.

Then record every finding in `review.md`:

```markdown
| ID | Finding | Decision | Reason | Where |
|---|---|---|---|---|
| R-1 | Unlimited approve on 0x3600 shown as a number | tech debt | display only | issue link |
| R-2 | Pending written after submit | fixed | double payment on lost response | commit |
| R-3 | No failover on node error | rejected | same as every other chain; tracked separately | issue link |
```

## 5. Hand-off and the paired platform

The iOS developer ports from our code and has repeatedly found defects that
apply to both apps. Make the loop explicit:

- Send the iOS developer the `design.md`, `test-vectors.md`, and `review.md`
  links together with the PR.
- When iOS reports a finding on their side of a chain we built, check it
  against our code in the same week and either fix it or record it in
  `review.md`. The same goes the other way.
- The test-vector file is shared: the iOS port records its results in its own
  column.
