# Litecoin MWEB light-client spike

Measured 2026-10-30 with `mwebd` v0.1.19 (github.com/ltcmweb/mwebd), the Go
MWEB daemon that Cake Wallet, Stack Wallet, Litewallet and Electrum-LTC embed.
Question answered here: what does the first sync and the steady state cost on
a phone, and is a header-checkpoint start worth building before the kit.

## What the light client does

`mwebd` embeds `ltcmweb/neutrino`. Sync order (from its `blockmanager.go` and
`mwebhandler.go`):

1. **Block headers from genesis**, 80 bytes each, in parallel from all peers,
   with scrypt proof-of-work checked for every header (a worker pool,
   `runtime.NumCPU()*2`). Litecoin tip at the time: 3,204,301.
2. **Compact-filter headers** (BIP157) for every block, 32 bytes each, synced
   in parallel with (1) from checkpoint lists the peers return. `mwebd`
   never uses the filters; this is inherited neutrino behaviour.
3. **MWEB headers**: every 100th block from MWEB activation (2,265,984) plus
   every block of the last 4000. Used only to map leaf indices to rough
   heights.
4. **Leafset + UTXO set** at the tip: one `mwebheader` + `mwebleafset`
   getdata, then `getmwebutxos` pages (compact format, up to 4096 per page,
   10 in flight), each verified against the output MMR root.
5. **Per new block**: `mwebheader` + full `mwebleafset` again, then only the
   leaves added since; removed leaves mark spends.

Servers (`litecoin-project/litecoin` `net_processing.cpp`) only answer
leafset/UTXO requests for blocks within 10 of their tip, cap a page at 4096
UTXOs, and since v0.21.5.6 rate-limit these requests node-wide
(32-token bucket, 0.5/s refill).

## Setup

- Binary: `go build -trimpath ./cmd/mwebd` (cgo: `ltcmweb/secp256k1` is a C
  library), macOS arm64 26.5 MB; Android arm64 via NDK r27
  `aarch64-linux-android26-clang`, `-ldflags "-s -w"`,
  `-Wl,-z,max-page-size=16384`: 19.8 MB.
- Runs: MacBook (Apple Silicon, Wi-Fi), Samsung Galaxy S21 (SM-G996B, Android
  15, Wi-Fi, USB-charging), Android emulator (arm64, API 36, 2.5 GB RAM, host
  network). All three started from an empty data dir at the same time against
  mainnet with default peer discovery.
- Measured every 30 s: `Rpc.Status` heights over gRPC, data-dir size, process
  CPU time, bytes received (per-process `nettop` on macOS; interface counters
  from `/proc/net/dev` on Android, so device numbers include other traffic).

## Results

Tip at start: 3,204,301. All runs from an empty data dir, mainnet, default
peer discovery (13 Litecoin Core 0.21.3-0.21.5.8 peers on the Mac).

### First sync

| | MacBook (Apple Silicon) | Galaxy S21 | Emulator (arm64) |
|---|---|---|---|
| Block headers synced (3.204M) | 6 min 07 s | 29 min 15 s | 11 min 09 s |
| MWEB headers (14,284 msgs) + leafset + UTXO set | 51 s | 45 s | 1 min 12 s |
| **Total to fully synced** | **7 min 07 s** | **30 min 00 s** | **12 min 21 s** |
| Bytes received | 394 MB (per process) | ~440 MB (uid 2000 via `dumpsys netstats`, minus ~25 MB adb) | ~410 MB from t=115 s on (interface-wide, first 449k headers not counted) |
| Bytes sent | 0.8 MB | | |
| CPU time | 11 min 19 s (multi-core; scrypt on every header) | 27 min 21 s on 8 cores (~1 core busy on average: not CPU-bound) | 10 min 58 s on 4 vCPUs |
| Data dir after sync | 689 MB | 752 MB (headers 251, cfheaders 100, bbolt 391) | 748 MB (headers 250, cfheaders 100, bbolt 391) |

Data dir breakdown (Mac): `block_headers.bin` 256 MB (3.204M x 80 B),
`reg_filter_headers.bin` 103 MB (x 32 B), `neutrino.db` (bbolt: header
index + MWEB coins) 305 MB, logs 7 MB.

On the S21 the header phase ran at ~1.8k headers/s against ~8.7k/s on the Mac
while using about one core; the limit on the phone is flash writes to the
bbolt index and/or peer bandwidth, not scrypt. Battery rose from 16% to 34%
on USB during the run (consumption not isolated); temperature 36.6 C at the end.

MWEB-specific data at height 3,204,305: 59,246 unspent outputs, ~384k
leaves ever created (leafset bitset ~48 KB), fetched in 15 `getmwebutxos`
pages in 10 s. The 14,284 MWEB headers took ~32 s. The MWEB part of the
first sync is small; **the cost is the header chain and its bbolt index.**

### Steady state at tip (Mac, per-process `nettop` deltas)

Window 18:53-19:12 (1150 s), 5 new blocks: 3.25 MB in, 0.15 MB out.

- **Per new block: ~400 KB in** (bursts of 360-415 KB). `mwebhandler.go` sends the
  `mwebheader` + `mwebleafset` getdata to *all* connected peers
  (`queryAllPeers`) and keeps the first verified answer, so the ~48 KB leafset
  (384k leaves / 8) arrives once per peer, 8 peers here. The MWEB header,
  the new UTXOs (~130 B each, compact) and the header/cfheader for the block
  are a few KB. Fix in the fork: query one peer and fall back; that brings a
  block to ~55 KB. The leafset grows with total MWEB outputs ever created
  (1 bit each), so it is ~48 KB today and would be ~125 KB at 1M outputs.
- **Idle: ~1.3 KB/s** (13.4 KB per 10 s) of transaction `inv` relay from 8
  peers. Neutrino already has a blocks-only mode (`Config.BlocksOnly` ->
  `DisableRelayTx` in the version message) that mwebd does not enable; a
  one-line change in our Go wrapper, or an upstream flag in `ServerArgs`,
  removes most of it.
  At 1.3 KB/s an always-open app would pull ~110 MB/day; the wallet syncs
  in the foreground only, so the realistic figure is per session.

Daily cost if left open all day without the two fixes: 576 blocks x ~400 KB
+ idle ~110 MB = ~340 MB/day. With the fixes: ~35 MB/day.

### Restart after a gap (emulator)

Stopped the synced daemon for 7 minutes (2 blocks), restarted: fully synced
again in **5 s**, ~570 KB device-wide (the leafset round from each peer, the
two missing headers, one new UTXO). The client always re-anchors at the
current tip, so a longer gap adds only the missing block headers and filter
headers (112 B per block, ~65 KB/day) plus the stride-1 MWEB headers of the
last-4000-block window (~1 KB each, up to ~4 MB after a week away). A week
offline is a few-second catch-up, not a rescan.

## Checkpoint-start feasibility

Today the fork has no way to start anywhere but genesis:

- `headerfs.NewBlockHeaderStore` writes the genesis header into an empty
  store; headers live in a flat file addressed as `height * 80`, with a
  bbolt index hash->height.
- `ltcd`'s newest hard-coded Litecoin checkpoint is height 721,000 (2015).
  Checkpoints only pin hashes; headers below them are still downloaded and
  scrypt-checked.
- Header validation (`checkHeaderSanity` -> `CheckBlockHeaderContext`) walks
  `RelativeAncestorCtx` back through the store for the retarget window
  (2016 blocks), so a seeded store needs the window before the start height.
- cfheaders sync starts from `filterHeaderTip` (genesis) and verifies against
  checkpoint lists peers return.
- MWEB header sync reads block hashes by height from the store from the
  activation height 2,265,984 (stride 100), so a start above activation
  loses the rough-height map for older UTXOs unless it is bundled.

A patch that makes first sync cheap:

1. Ship a checkpoint bundle per network: the 2017 block headers ending at
   height H (161 KB), the filter header at H (32 bytes), and optionally the
   stride-100 MWEB `leavesAtHeight` map from activation to H (~9.4k entries,
   ~110 KB). H is refreshed at kit release time, as bitcoinkit does.
2. On an empty store, write those headers at their real offsets (sparse file;
   fine on ext4/f2fs/APFS) and index them; set `filterHeaderTip` = H.
3. Start header sync from H; `RelativeAncestorCtx` finds its 2016 ancestors;
   validation code is unchanged.
4. `getMwebHeaders` starts at max(activation, H-4000) when the bundle has
   the map, else at H; UTXOs older than the first mapped height display as
   "at or before H".

Scope: ~300-500 lines in `headerfs/store.go`, `neutrino.go` (config),
`blockmanager.go` (start heights), `mwebheaders.go`, plus a bundle generator.
Trust model identical to bitcoinkit's checkpoints. Download after the patch:
headers from H to tip only (tens of MB per month of lag) plus the MWEB data.
Worth proposing upstream to `ltcmweb/neutrino` as an optional config.

## Recommendation

MWEB on a phone is usable: Cake ships this exact stack, and our S21 run
proves a full first sync in 30 minutes on Wi-Fi with the phone otherwise
idle, then second-level catch-ups. But the first-sync cost is real and it is
almost entirely the Litecoin header chain, which no wallet birthday avoids:

- ~400-440 MB downloaded once per device;
- **690-750 MB of permanent storage** (headers 250, filter headers 100,
  bbolt index 300-390), more than any other coin we ship keeps;
- 7 min (laptop) to 30 min (S21) wall time, 11-27 min CPU.

Decisions:

(a) **Checkpoint-start patch: go.** It is the only lever that shrinks all
three numbers at once, to tens of MB and a couple of minutes. Scope is
~300-500 lines in the neutrino fork plus a bundle generator (see feasibility
above). Build it in Phase 1 as part of the kit's Go layer, propose it
upstream, and keep the genesis path as fallback.

Also in the Go layer, two small fixes found here: request the leafset from
one peer instead of all (cuts per-block traffic from ~400 KB to ~55 KB) and
enable neutrino's blocks-only mode (cuts idle traffic from 1.3 KB/s to near
zero). Both are upstreamable.

(b) **Shared per-device data dir: yes.** One header chain for all Litecoin
MWEB wallets on the device, as Cake does.

(c) **Opt-in per wallet with a size notice: yes**, at least until the
checkpoint start lands; the notice can then shrink to a one-liner.

Also noted for the kit: the header phase on the phone was not CPU-bound
(one core busy), so the sync should run in a foreground service without
worrying about thermal throttling; and `mwebd` stops itself when its parent
process exits, which the JNI wrapper must account for (we call
`NewServer2`/`Start` directly, not the `cmd/mwebd` binary, so this does
not apply in-app).

## Reproducing

Scripts and logs from this run are in the session scratchpad
(`mweb-spike/`: `poll.sh`, `poll-device.sh`, `netloop.sh`, `logs/*.csv`).
Build: `go build -trimpath ./cmd/mwebd` in a checkout of
`github.com/ltcmweb/mwebd` at v0.1.19; for Android
`CGO_ENABLED=1 GOOS=android GOARCH=arm64 CC=$NDK/.../aarch64-linux-android26-clang`.
Run: `mwebd -c mainnet -d <datadir> -l 127.0.0.1:12345`; poll with
`grpcurl -plaintext -import-path proto -proto mwebd.proto 127.0.0.1:12345 Rpc.Status`.
