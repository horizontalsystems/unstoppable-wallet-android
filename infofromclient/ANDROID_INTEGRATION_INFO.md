# Oxyra X (OXRX) - Android Integration Information

## 1. RPC Node Access

### Public RPC Endpoint
```
URL: https://monero.bad-abda.online/
```

**Available RPC Methods:**
- `get_balance` - Check wallet balance
- `transfer` - Send transactions
- `get_height` - Get blockchain height
- `get_block` - Retrieve block information
- And all standard Monero-wallet-RPC compatible methods

---

## 2. Chain/Network Parameters

### Basic Information
| Parameter | Value | Description |
|-----------|-------|-------------|
| **Network Name** | oxyra | Internal network identifier |
| **Coin Symbol** | OXRX | Trading symbol |
| **Network Type** | mainnet | Production network |
| **Decimals** | 12 | Decimal places (1 OXRX = 10^12 atomic units) |

### Network Ports
| Service | Port | Description |
|---------|------|-------------|
| **P2P Port** | 18080 | Peer-to-peer communication |
| **RPC Port** | 18081 | RPC API endpoint |
| **ZMQ Port** | 18082 | ZeroMQ notifications |

### Network ID (UUID)
```
Mainnet: 65:45:33:ED:F3:22:47:AB:BA:C8:94:5A:A8:31:EB:4F
```

### Address Prefixes (Base58)
| Address Type | Prefix | Description |
|--------------|--------|-------------|
| **Standard Address** | 18 | Regular wallet addresses |
| **Integrated Address** | 19 | Address with payment ID |
| **Subaddress** | 42 | Subaddress for privacy |

### Genesis Transaction
```
TX Hex: 013c01ff00018080b0b1af8389d129020c0045d8c6ed4d609e90e19ec8d9e188b5cd03a4cf8bf94f126cc61796cb967f2101ad615f1095fd6f56e759a61fe1b73b73904ab4339869dd38211c27a08e4284d9
Genesis Nonce: 10000
```

### Genesis Hash
```
To calculate: Use the daemon command to get genesis block hash:
./oxyrad print_genesis_tx

Or query via RPC:
curl https://monero.bad-abda.online/json_rpc -d '{"jsonrpc":"2.0","id":"0","method":"get_block","params":{"height":0}}' -H 'Content-Type: application/json'
```

---

## 3. Blockchain Parameters

### Supply & Emission
| Parameter | Value |
|-----------|-------|
| **Total Supply** | 3,000,000,000 OXRX (3 billion) |
| **Atomic Units** | 3,000,000,000,000,000,000 (3×10^21) |
| **Emission Type** | Pre-mined (all coins created at genesis) |
| **Block Reward** | 0 (no new coins after genesis) |

### Timing & Difficulty
| Parameter | Value |
|-----------|-------|
| **Block Time** | 120 seconds (2 minutes) |
| **Difficulty Window** | 720 blocks |
| **Unlock Time** | 60 blocks (~2 hours) |

### Transaction Parameters
| Parameter | Value |
|-----------|-------|
| **Min Fee** | 2,000,000,000 atomic units (0.002 OXRX) |
| **Fee Per Byte** | 300,000 atomic units |
| **Dust Threshold** | 2,000,000,000 atomic units |
| **Max TX Size** | 1,000,000 bytes (1 MB) |

---

## 4. Seed Nodes

### DNS Seed Nodes
```
seeds.moneroseeds.se
seeds.moneroseeds.ae.org
seeds.moneroseeds.ch
seeds.moneroseeds.li
```

**Note:** These are default Monero seed nodes. For Oxyra X custom network, you should configure your own seed nodes. Currently the hardcoded IP seed nodes are commented out in the code.

### Recommended Seed Node Configuration
```
# You should set up and provide:
seed1.oxyrax.net:18080
seed2.oxyrax.net:18080
seed3.oxyrax.net:18080
```

---

## 5. Integration Config File (JSON)

### Complete Configuration
```json
{
  "id": "oxyrax",
  "name": "Oxyra X",
  "symbol": "OXRX",
  "decimals": 12,
  "network": "mainnet",
  
  "rpc": {
    "url": "https://monero.bad-abda.online/",
    "port": 18081,
    "zmq_port": 18082
  },
  
  "explorer": {
    "url": "https://explorer.oxyrax.io",
    "tx_url": "https://explorer.oxyrax.io/tx/",
    "block_url": "https://explorer.oxyrax.io/block/"
  },
  
  "network_params": {
    "network_id": "6545-33ED-F322-47AB-BAC8-945A-A831-EB4F",
    "p2p_port": 18080,
    "rpc_port": 18081,
    "block_time": 120,
    "unlock_blocks": 60
  },
  
  "address_prefixes": {
    "standard": 18,
    "integrated": 19,
    "subaddress": 42
  },
  
  "genesis": {
    "tx": "013c01ff00018080b0b1af8389d129020c0045d8c6ed4d609e90e19ec8d9e188b5cd03a4cf8bf94f126cc61796cb967f2101ad615f1095fd6f56e759a61fe1b73b73904ab4339869dd38211c27a08e4284d9",
    "nonce": 10000
  },
  
  "seed_nodes": [
    "seeds.moneroseeds.se:18080",
    "seeds.moneroseeds.ae.org:18080",
    "seeds.moneroseeds.ch:18080",
    "seeds.moneroseeds.li:18080"
  ],
  
  "fees": {
    "default_per_kb": 2000000000,
    "per_byte": 300000,
    "dust_threshold": 2000000000
  }
}
```

---

## 6. Wallet RPC Specification

### Compatible with Monero Wallet RPC

The Oxyra X network is fully compatible with `monero-wallet-rpc` commands. Below are the most commonly used methods:

#### Balance Check
```bash
curl -X POST https://monero.bad-abda.online/json_rpc \
  -d '{"jsonrpc":"2.0","id":"0","method":"get_balance","params":{"account_index":0}}' \
  -H 'Content-Type: application/json'
```

#### Transfer (Send Transaction)
```bash
curl -X POST https://monero.bad-abda.online/json_rpc \
  -d '{
    "jsonrpc":"2.0",
    "id":"0",
    "method":"transfer",
    "params":{
      "destinations":[{"amount":1000000000000,"address":"OXRX_ADDRESS_HERE"}],
      "priority":0,
      "ring_size":16,
      "get_tx_key":true
    }
  }' \
  -H 'Content-Type: application/json'
```

#### Get Address
```bash
curl -X POST https://monero.bad-abda.online/json_rpc \
  -d '{"jsonrpc":"2.0","id":"0","method":"get_address","params":{"account_index":0}}' \
  -H 'Content-Type: application/json'
```

#### Get Transaction History
```bash
curl -X POST https://monero.bad-abda.online/json_rpc \
  -d '{"jsonrpc":"2.0","id":"0","method":"get_transfers","params":{"in":true,"out":true}}' \
  -H 'Content-Type: application/json'
```

### Full RPC Method List
- `get_balance` - Get wallet balance
- `get_address` - Get wallet addresses
- `create_address` - Create new address
- `transfer` - Send OXRX
- `transfer_split` - Send OXRX with automatic splitting
- `get_transfers` - Get transaction history
- `get_transfer_by_txid` - Get specific transaction
- `incoming_transfers` - Get incoming transfers
- `query_key` - Get wallet keys
- `make_integrated_address` - Create integrated address
- `split_integrated_address` - Parse integrated address
- `get_height` - Get blockchain height
- `validate_address` - Validate address format

---

## 7. Testing & Verification

### Verify RPC Connection
```bash
# Test daemon connectivity
curl https://monero.bad-abda.online/json_rpc \
  -d '{"jsonrpc":"2.0","id":"0","method":"get_info"}' \
  -H 'Content-Type: application/json'

# Expected response includes:
# - height: current blockchain height
# - difficulty: current network difficulty
# - network_type: should be "mainnet"
```

### Verify Genesis Block
```bash
# Get genesis block (height 0)
curl https://monero.bad-abda.online/json_rpc \
  -d '{"jsonrpc":"2.0","id":"0","method":"get_block","params":{"height":0}}' \
  -H 'Content-Type: application/json'

# The genesis block hash should match your expected value
```

---

## 8. Important Notes for Android Development

### Privacy Features
- Oxyra X uses **Ring Signatures** (minimum ring size: varies by hard fork version)
- **Stealth Addresses** for recipient privacy
- **RingCT** for amount confidentiality
- **View Tags** for wallet scanning optimization (HF15+)

### Hard Fork Schedule
The network supports multiple hard fork versions with different features:
- HF 6: Minimum mixin 4, RCT enforcement
- HF 8: Per-byte fees, minimum mixin 10
- HF 10: Smaller bulletproofs, long-term block weight
- HF 12: Minimum 2 outputs, same mixin enforcement
- HF 13: CLSAG signatures, deterministic unlock time
- HF 15: Bulletproof+, view tags, 2021 scaling

### Security Considerations
1. **SSL/TLS**: The RPC endpoint should use HTTPS
2. **Authentication**: May require RPC username/password
3. **Rate Limiting**: RPC may have connection limits (default: 100 concurrent)
4. **Wallet Security**: Store private keys securely using Android Keystore

### Recommended Libraries
- **Monero Core**: C++ library (can be cross-compiled for Android)
- **Monerujo**: Open-source Android Monero wallet (good reference)
- **XMRig**: For reference on cryptographic operations

---

## 9. Additional Resources

### Daemon Command Line
```bash
# Start daemon
./oxyrad --rpc-bind-ip 0.0.0.0 --rpc-bind-port 18081 --confirm-external-bind

# Start wallet RPC
./oxyrax-wallet-rpc --wallet-file /path/to/wallet --password "PASSWORD" \
  --rpc-bind-port 18082 --daemon-address localhost:18081
```

### Build Information
- Based on Monero codebase
- Fully open source
- Compatible with Monero tooling and libraries

---

## 10. Contact & Support

For technical questions and support:
- Provide your technical support contacts here
- GitHub repository (if public)
- Developer documentation URL
- API documentation URL

---

## Summary Checklist for Android Developer

✅ **RPC Endpoint**: `https://monero.bad-abda.online/`  
✅ **Network ID**: `6545-33ED-F322-47AB-BAC8-945A-A831-EB4F`  
✅ **Ports**: P2P=18080, RPC=18081, ZMQ=18082  
✅ **Symbol**: OXRX  
✅ **Decimals**: 12  
✅ **Address Prefix**: 18 (standard), 19 (integrated), 42 (subaddress)  
✅ **Seed Nodes**: seeds.moneroseeds.se, seeds.moneroseeds.ae.org, etc.  
✅ **RPC Compatibility**: Full Monero wallet-RPC support  
✅ **Genesis TX**: Available in config  
⚠️ **Genesis Hash**: Calculate using daemon or RPC call  
⚠️ **Custom Seed Nodes**: Recommended to set up your own

---

**Document Version**: 1.0  
**Last Updated**: October 13, 2025  
**Generated From**: Oxyra X source code @ /home/Oxyra

