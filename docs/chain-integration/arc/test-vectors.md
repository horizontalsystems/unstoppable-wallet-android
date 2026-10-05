# Arc: native USDC history test vectors

Checks the history of native USDC on Arc (chain ID 5042): transfers through
the ERC-20 interface `0x3600000000000000000000000000000000000000` show in the
USDC wallet, once, at the right scale. Written by the iOS team on 2026-09-22
against their port of our Arc support.

Addresses are third-party and live; add them as watch accounts (EVM address).
Chain snapshot: 2026-09-22 05:20 UTC. Transactions newer than the snapshot
are not part of the vector. Times are UTC; the app shows local time, so dates
may shift.

## Mechanics under test

| On the chain | What the wallet must show | Where in code |
|---|---|---|
| USDC is the native coin (18 decimals). `0x3600…0000` is its ERC-20 interface (6 decimals) over the same balance | A transfer through `0x3600` is a native USDC movement, rescaled 6 → 18 | `EvmTransactionConverter.getEip20Value` |
| The kit tags a `0x3600` transfer with the contract, not as native | The USDC wallet queries history by both tags: native and `0x3600` | `EvmTransactionsAdapter` tag queries |
| A transaction can match both tags | One row, no duplicates | EvmKit transaction storage |
| Every native movement also emits a `Transfer` event from `0xffff…fffe` | The event is dropped; no extra row | `EvmTransactionConverter.isNativeTransferLog` |
| `0x3600` and `0xffff…fffe` are not tokens | Never become a separate wallet | `BlockchainType.blockedEip20Addresses` |
| Fee-only transaction (value 0) | Not in the USDC wallet; visible in "All" | EvmKit tags |
| Incoming USDC below 0.1 | Hidden as spam | `SpamManager` |

Criteria for every address:

1. No hash appears twice in the USDC wallet, in "All", or under the incoming
   and outgoing filters.
2. `0x3600` amounts are at normal scale (35.7, not 0.0000000000357).
3. Fake tokens with the USDC or EURC ticker never mix with native USDC and are
   not shown as a normal incoming.

## Address 1 — transfers only through `0x3600`

`0x9a3f13def17b573be2540136430cd06df92a6818` · 6 transactions on chain · 5
expected in the USDC wallet

No transaction carries a native value; all movement is through the interface.
Without the second tag the USDC wallet would show nothing.

| UTC | Hash | On chain | In USDC wallet | Why |
|---|---|---|---|---|
| 09-22 04:51 | `0xddb3ff885c89e49bbb50b77cab9335d37588c4eeee942231e49b07c35f7a0ed7` | direct `transfer` on `0x3600`, −35.719684 | −35.7 | `0x3600` tag |
| 09-21 22:00 | `0x30dfc0e85d75edc23d136fc141c8a113e198c69366e2042d8e468683a7246c0a` | incoming via `0x3600`, +10.702931 | +10.7 | `0x3600` tag |
| 09-21 12:40 | `0x93847223c809edf6c1e1e85061586f7257a922ee82e61da94194a936a85792e8` | call to `0x4cd00e…`, 25.055 taken via `0x3600` | −25 | `0x3600` tag |
| 09-21 12:40 | `0x9efab5d8fdbd48f16bd4ef2ad983f11596d4268e8bf74cd505c5abc7c50c4f65` | `approve` on `0x3600`, 25.055 for `0x4cd00e…` | approve 25 | `0x3600` tag; no money moves |
| 09-17 12:51 | `0x04944106fa044994fa7afc79e8fafe74e9443c7652022afb5dedb34978c02700` | incoming via `0x3600`, +50.1 | +50.1 | `0x3600` tag |
| 09-17 12:46 | `0xba0cd131d2b263e0d33079d007cdf32278771e063bac3950081e49e91aa78e87` | incoming via `0x3600`, +0.01 | **hidden** | spam: 0.01 < 0.1 USDC |

## Address 2 — native amounts, `0x3600`, and NFTs

`0xaf890b225514ffbd5a58eea563ee493760e9c4f3` · 13 transactions on chain · 9
expected in the USDC wallet

| UTC | Hash | On chain | In USDC wallet | Why |
|---|---|---|---|---|
| 09-22 04:51 | `0x35b5c301d69a30cfdb55252c2433b8c23d3c6469014579624c0b8526c7d896b0` | direct `transfer` on `0x3600`, −0.14509 | −0.145 | `0x3600` tag |
| 09-16 20:19 | `0x95b0c1792af4ea14039eecc3ccf44621a834e13d183c1e3ef5ee2a36d1f32557` | incoming 100 PUNK | no | other token; in "All" hidden as spam (unknown ERC-20) |
| 09-16 14:51 | `0x616fdd7415105793eef717d0699bab086012db403b7a6f11a2066688ef79ee7e` | incoming via `0x3600`, +6.061279 | +6.06 | `0x3600` tag |
| 09-16 09:22 | `0xcaa5fac3387bc09d349645b10b6622511231212a6b69e87091d8ddcc7cd2f6bd` | buys 2 NFTs: **two** transfers of 0.03 via `0x3600` | −0.06, **one** row | two events of one contract in one transaction merge |
| 09-16 08:16 | `0x41f985feef72b5a849351060b1723851a6834d5ffdbca9bcbdbc7d80184d83f1` | `approve` on `0x3600`, 0.06 | approve 0.06 | `0x3600` tag; no money moves |
| 09-16 08:13 | `0x20aca08cfad5d24e98c0af9f6b1fa4d4d595de50433c1861df5925b80130a2d3` | buys NFT, 0.08 via `0x3600` | −0.08 | `0x3600` tag |
| 09-16 08:10 | `0x828aed039dadffde13167a39c54bdd00fcb4c7928c96640649c8c12c6a38f491` | `approve` on `0x3600`, 0.08 | approve 0.08 | `0x3600` tag; no money moves |
| 09-16 07:55 | `0x441423e1dcbad62896c65023f7d113c90277cad648ea3208083c48b3c24b8c08` | `setApprovalForAll` on an NFT collection | no | fee only; visible in "All" |
| 09-16 07:54 | `0x5847cc503e082550e7eecfa932e17d4765a232f3553de6f6f3c0f6002fe87d2b` | free mint of 2 NFTs | no | fee only; visible in "All" |
| 09-16 05:06 | `0xf8c2e82e785c0675ab2c152c589f1f346770c1ca8e233fc202634fba67f0ba06` | NFT mint for native 0.15 (+ mirror event) | −0.15, one row | native tag; mirror dropped |
| 09-16 04:55 | `0xfda6cd17a69b15ad1f17dc601768a08f4b9f7eb48693923c00d5af0d205679b1` | NFT mint for native 0.5 (+ mirror) | −0.5, one row | native tag; mirror dropped |
| 09-16 04:50 | `0x385f806865f0e464044722c4120e91bdd9babc8aadaa0d517cb46b3347badc28` | free NFT mint | no | fee only; visible in "All" |
| 09-16 03:52 | `0x55ee03f395663f2aeda40dad9936e0a79151c69b34f693dbabc5e6fda260d897` | incoming via `0x3600`, +1.0 | +1 | `0x3600` tag |

## Address 3 — token trades, internal transactions, address poisoning

`0xefd9bb7f679314b43fe5e47816b8634927fbcc10` · 10 transactions on chain · 6
expected in the USDC wallet

| UTC | Hash | On chain | In USDC wallet | Why |
|---|---|---|---|---|
| 09-22 05:20 | `0xf8f3933bfb33bc80c5de0b9b979899295240422a6eaf82bd04ff2285645eb6be` | **poisoning**: fake "USDC" `0x298d4a…` emits "617.183615 to `0xc20617…`" (real recipient `0xc206cf…`) | no | other contract; in "All" hidden or marked suspicious |
| 09-22 04:52 | `0xfe4a2b0e2c03f7e78ea8f90e3333d7c16b16e09493d5fe7c6bc8090aaacf3419` | direct `transfer` on `0x3600`, −617.183615 to `0xc206cf…` | −617, recipient `0xc206cf…` | `0x3600` tag |
| 09-18 09:41 | `0x37da8c5230d5a66590249782139dc58eaf865416d18d897b4e03dd95989907ae` | incoming 32 fake "EURC" `0x172c0b…` (18 decimals; real EURC is `0xbef5f6d5…`, 6 decimals) | no | other token; must not become an EURC wallet |
| 09-18 08:04 | `0xb7508e564ce8d99ac523bef6c5996468fd74e436d37965476b0af424a58137b3` | sells Architects; USDC arrives as internal transaction 7.81414821 (+ mirror) | +7.81, one row | native tag from internal transaction; mirror dropped |
| 09-18 08:04 | `0x5027e71841791eb18bb72af2687f5fdb2923ef52923b81eaaf8ef704f8eea7a6` | `approve` of Architects | no | not USDC; visible in "All" |
| 09-17 05:38 | `0xfc9a77ea0adb6f5d471dc0fb3a2cc2920fe9021f80ace073711dfc8a037fd0a1` | sells ARCANINE, internal transaction 9.95040189 (+ mirror) | +9.95, one row | native tag from internal transaction; mirror dropped |
| 09-17 05:38 | `0x818463914828ad2e6a6e7009fb6cc530da7952536f34e207ebde719ec59ebf1c` | `approve` of ARCANINE | no | not USDC; visible in "All" |
| 09-16 03:21 | `0x4e00f612059329520e45595e73db3f4beaee91d10fc8b163cd86e2181bea21dd` | buys ARCANINE for native 50 (+ mirror) | −50 swap | native tag; mirror dropped |
| 09-16 03:10 | `0xb585ef7eacf9e8195763cd3289c0e239d45fbd21656e24112a23b8f0d6ca10cc` | buys Architects for native 50 (+ mirror) | −50 swap | native tag; mirror dropped |
| 09-16 02:53 | `0x554e9904dcf499d1746b1cc012b79069d94829e135ad7e2da498d0725300cd95` | plain native transfer +699.9536 from `0xc206cf…` (+ mirror) | +699 | native tag; mirror dropped |

## Address 4 — plain native transfers, control

`0x5e48131a8f62ceaaf5c2b75f69ded3542964e181` · 14 transactions on chain · 12
expected in the USDC wallet

The second tag adds nothing here. Checks that plain transfers are not doubled
by the mirror event and that a fake "USDC" does not mix with the native one.

| UTC | Hash | On chain | In USDC wallet | Why |
|---|---|---|---|---|
| 09-22 04:51 | `0x41d79398eea677d5c144b3fe319cbfc26bbdc2b55ede1e3597024249731f888b` | −349.99916 to `0x411603…` | −349.99916 | native; mirror dropped |
| 09-22 04:51 | `0x28d76b80177428ed0ef906986705471a3b605b4a35856abbf2430c7d2cdec01a` | +350 from `0x2d548a…` | +350 | native |
| 09-21 20:57 | `0x33cb9def5ae6a635a07dc56126c79aad1c6c5ae4d2e911e1081b5f61ca8b0f80` | −199.99916 | −199.99916 | native |
| 09-21 20:55 | `0xb7d7bf28564c6c399211f73812141e9f18b1dfe6cb2ccb0ecf26fa7096f84e11` | +200 | +200 | native |
| 09-21 17:03 | `0xa037dc835f76a50a41553ef54d6936f72e1773ab03d32a086daa20fbe7cec05a` | −349.99916 | −349.99916 | native |
| 09-21 16:58 | `0x587ecc02acbb26aa90cbfd04259c5309b918fd639bacbf251e92543c98d3f459` | +350 | +350 | native |
| 09-21 09:00 | `0x447b28f20b9753f9b945b8629e08e3759094f6d15eb80e2d9489558654dcaf20` | −299.99916 | −299.99916 | native |
| 09-21 08:57 | `0xca7aafd3c308f699d5545744559ba317d89d1034783f76987497e46642d2133d` | +300 | +300 | native |
| 09-20 15:17 | `0x18d0c5987003305c98af10a47f8180d8f414362ee3c60fe7545ea28853253e9f` | **poisoning**: fake "USDC" `0xc6203d…` (18 decimals), "+100 from `0x2d545b…`" (real sender `0x2d548a…`) | no | other contract; in "All" hidden or marked |
| 09-20 15:06 | `0x048f099f990ffbbdb0d020d2b464faa48f63f6b9d4bf92880984db8e54e7e47a` | −99.99916 | −99.99916 | native |
| 09-20 15:06 | `0x685b6a05b5e8bd68e49d2eacb418061005a19017a4d9978f70b7cb8fd6977bb7` | +100 | +100 | native |
| 09-18 09:57 | `0x28961a8ff0ea537ecf525c8de0d5a03c070b8ca56a4874676fe15fc5be76da93` | **poisoning**: fake "USDC" `0xc6203d…`, "+53.679746382 from `0x05fd39…`" (real sender `0x05fd9e…`) | no | other contract; in "All" hidden or marked |
| 09-18 09:48 | `0xcaa92007c12f4e3141c010994b14444913b54b2c4cfa1a624420de34456b2f21` | −53.678906382 | −53.68 | native |
| 09-18 09:42 | `0xe01740aeed0127e2fa1d85343dd0dc69309311b2f11bfbac17f1702bef20d162` | +53.679746382 from `0x05fd9e…` | +53.68 | native |

## Runs

| Run | Platform | Build | Result |
|---|---|---|---|
| 2026-09-22 | iOS | Arc history fix | passed in the USDC wallet for all four addresses |
| — | Android | — | not run: vectors were written for the iOS port and have not been checked on Android yet |

## Not verified

- **The "All" tab** for addresses 2–4: the three poisoning transactions
  (`0xf8f3933b…`, `0x18d0c598…`, `0x28961a8f…`), the fake EURC, and PUNK must
  be hidden or marked suspicious. Showing them as a normal incoming is a spam
  filter defect.
- **Approve display** (address 1: `0x9efab5d8…`; address 2: `0x41f985fe…`,
  `0x828aed03…`): must read as an approve, not as "+0.08" next to a purchase
  of the same amount.
- **Unlimited approve on `0x3600`**: not in these addresses. Known defect on
  both platforms: the amount is rescaled by 6 decimals but checked for the
  maximum against 18, so it shows a huge number instead of ∞ (checklist
  AMT-2).
- **A transaction with both tags at once** (native value and a `0x3600`
  transfer) is not represented: the on-chain examples found belong to bots
  with 100+ transactions. Deduplication for this case is confirmed only by
  reading the code.
