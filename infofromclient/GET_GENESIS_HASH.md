# How to Get Genesis Hash for Oxyra X

## Method 1: Using RPC API (Recommended for Android)

### Get Genesis Block
```bash
curl -X POST https://monero.bad-abda.online/json_rpc \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "id": "0",
    "method": "get_block",
    "params": {
      "height": 0
    }
  }'
```

### Expected Response
```json
{
  "id": "0",
  "jsonrpc": "2.0",
  "result": {
    "blob": "...",
    "block_header": {
      "block_size": 106,
      "depth": <current_height>,
      "difficulty": 1,
      "hash": "GENESIS_HASH_HERE",
      "height": 0,
      "major_version": 1,
      "minor_version": 0,
      "nonce": 10000,
      "num_txs": 1,
      "orphan_status": false,
      "prev_hash": "0000000000000000000000000000000000000000000000000000000000000000",
      "reward": 0,
      "timestamp": <genesis_timestamp>
    },
    "json": "...",
    "miner_tx_hash": "...",
    "tx_hashes": []
  }
}
```

The `hash` field in `block_header` is your **GENESIS_HASH**.

---

## Method 2: Using Daemon Directly

If you have access to the compiled daemon:

```bash
# Start the daemon
./oxyrad --help

# Or query using RPC locally
./oxyrad --rpc-bind-port 18081 --detach

# Then query
curl http://localhost:18081/json_rpc \
  -d '{"jsonrpc":"2.0","id":"0","method":"get_block","params":{"height":0}}' \
  -H 'Content-Type: application/json'
```

---

## Method 3: Calculate from Genesis Transaction

The genesis hash is derived from the genesis block which contains:
- Genesis transaction: `013c01ff00018080b0b1af8389d129020c0045d8c6ed4d609e90e19ec8d9e188b5cd03a4cf8bf94f126cc61796cb967f2101ad615f1095fd6f56e759a61fe1b73b73904ab4339869dd38211c27a08e4284d9`
- Genesis nonce: `10000`
- Version: 1
- Timestamp: (genesis timestamp)

This requires the Monero/Oxyra cryptographic libraries to compute the hash properly.

---

## Quick Test Script

Save this as `get_genesis.sh`:

```bash
#!/bin/bash

echo "Fetching genesis block from Oxyra X node..."
echo ""

RESPONSE=$(curl -s -X POST https://monero.bad-abda.online/json_rpc \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "id": "0",
    "method": "get_block",
    "params": {
      "height": 0
    }
  }')

echo "Full Response:"
echo "$RESPONSE" | jq '.'
echo ""

GENESIS_HASH=$(echo "$RESPONSE" | jq -r '.result.block_header.hash')

echo "====================================="
echo "GENESIS HASH: $GENESIS_HASH"
echo "====================================="
echo ""
echo "Add this to your Android config:"
echo "\"genesis_hash\": \"$GENESIS_HASH\""
```

Make it executable and run:
```bash
chmod +x get_genesis.sh
./get_genesis.sh
```

**Note:** Requires `jq` for JSON parsing. Install with: `sudo apt install jq`

---

## For Android Integration

Once you have the genesis hash, add it to your config:

```json
{
  "genesis": {
    "tx": "013c01ff00018080b0b1af8389d129020c0045d8c6ed4d609e90e19ec8d9e188b5cd03a4cf8bf94f126cc61796cb967f2101ad615f1095fd6f56e759a61fe1b73b73904ab4339869dd38211c27a08e4284d9",
    "nonce": 10000,
    "hash": "YOUR_GENESIS_HASH_HERE"
  }
}
```

The genesis hash is used to:
1. Verify you're connecting to the correct network
2. Validate blockchain synchronization
3. Ensure the wallet is on the right chain

---

## Verification

To verify the genesis hash is correct:

1. Check that the hash is 64 hexadecimal characters (32 bytes)
2. Query the same hash from multiple nodes to confirm consistency
3. Verify the previous hash is all zeros (0000...0000)
4. Confirm the height is 0

```bash
# Verify from another RPC call
curl https://monero.bad-abda.online/get_block_header_by_height \
  -d '{"height": 0}' \
  -H 'Content-Type: application/json'
```

