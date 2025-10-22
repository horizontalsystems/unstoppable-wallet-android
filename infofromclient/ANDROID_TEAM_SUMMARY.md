# Oxyra X (OXRX) - Android Integration Quick Reference

## ✅ Ready to Share with Android Team

---

## 1. RPC Node Endpoint
```
https://monero.bad-abda.online/
```

---

## 2. Network Configuration

### Basic Parameters
| Parameter | Value |
|-----------|-------|
| **Coin Name** | Oxyra X |
| **Symbol** | OXRX |
| **Decimals** | 12 |
| **Network** | mainnet |

### Ports
| Service | Port |
|---------|------|
| P2P | 18080 |
| RPC | 18081 |
| ZMQ | 18082 |

---

## 3. Network Identity

### Network ID (UUID)
```
65:45:33:ED:F3:22:47:AB:BA:C8:94:5A:A8:31:EB:4F
```

**As byte array for Android:**
```java
byte[] NETWORK_ID = {
    (byte)0x65, (byte)0x45, (byte)0x33, (byte)0xED, 
    (byte)0xF3, (byte)0x22, (byte)0x47, (byte)0xAB,
    (byte)0xBA, (byte)0xC8, (byte)0x94, (byte)0x5A, 
    (byte)0xA8, (byte)0x31, (byte)0xEB, (byte)0x4F
};
```

---

## 4. Genesis Information

### Genesis Hash
```
8539ba68b5157a156575d5164d1e5c46ad97cb88679a4e64235bfe5a2437953f
```

### Genesis Transaction
```
013c01ff00018080b0b1af8389d129020c0045d8c6ed4d609e90e19ec8d9e188b5cd03a4cf8bf94f126cc61796cb967f2101ad615f1095fd6f56e759a61fe1b73b73904ab4339869dd38211c27a08e4284d9
```

### Genesis Nonce
```
10000
```

---

## 5. Address Prefixes

| Address Type | Prefix (Decimal) | Sample Address Start |
|--------------|------------------|----------------------|
| Standard | 18 | Starts with "4" |
| Integrated | 19 | Starts with "4" |
| Subaddress | 42 | Starts with "8" |

**Note:** Address prefixes determine how addresses look when encoded in Base58.

---

## 6. Seed Nodes

```
seeds.moneroseeds.se:18080
seeds.moneroseeds.ae.org:18080
seeds.moneroseeds.ch:18080
seeds.moneroseeds.li:18080
```

**For Android string array:**
```java
String[] SEED_NODES = {
    "seeds.moneroseeds.se:18080",
    "seeds.moneroseeds.ae.org:18080",
    "seeds.moneroseeds.ch:18080",
    "seeds.moneroseeds.li:18080"
};
```

---

## 7. Integration Config (JSON)

**File:** `oxyrax_android_config.json`

```json
{
  "id": "oxyrax",
  "name": "Oxyra X",
  "symbol": "OXRX",
  "decimals": 12,
  "rpcUrl": "https://monero.bad-abda.online/",
  "explorerUrl": "https://explorer.oxyrax.io",
  "network": "mainnet",
  "networkId": "6545-33ED-F322-47AB-BAC8-945A-A831-EB4F",
  "genesisHash": "8539ba68b5157a156575d5164d1e5c46ad97cb88679a4e64235bfe5a2437953f",
  "addressPrefix": 18,
  "p2pPort": 18080,
  "rpcPort": 18081
}
```

---

## 8. Wallet RPC Methods (Monero Compatible)

### Most Important Methods
- ✅ `get_balance` - Get wallet balance
- ✅ `get_address` - Get wallet addresses  
- ✅ `transfer` - Send OXRX
- ✅ `get_transfers` - Get transaction history
- ✅ `validate_address` - Validate address format
- ✅ `get_height` - Get blockchain height
- ✅ `create_address` - Create new subaddress
- ✅ `make_integrated_address` - Create integrated address

### Example API Call
```java
// Get balance
POST https://monero.bad-abda.online/json_rpc
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "0",
  "method": "get_balance",
  "params": {
    "account_index": 0
  }
}
```

---

## 9. Transaction Parameters

| Parameter | Value | Note |
|-----------|-------|------|
| **Block Time** | 120 seconds | 2 minutes per block |
| **Unlock Time** | 60 blocks | ~2 hours |
| **Min Fee** | 2,000,000,000 atomic units | 0.002 OXRX |
| **Dust Threshold** | 2,000,000,000 atomic units | 0.002 OXRX |
| **Ring Size** | 16 minimum | For privacy |

---

## 10. Coin Supply

| Item | Value |
|------|-------|
| **Total Supply** | 3,000,000,000 OXRX |
| **Atomic Units** | 3,000,000,000,000,000,000 |
| **Emission** | Pre-mined (all at genesis) |
| **Block Reward** | 0 (no new coins) |

---

## 11. Testing Connection

### Test RPC Connection
```bash
curl -X POST https://monero.bad-abda.online/json_rpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"0","method":"get_info"}'
```

### Verify Genesis Block
```bash
curl -X POST https://monero.bad-abda.online/json_rpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"0","method":"get_block","params":{"height":0}}'
```

**Expected genesis hash in response:**
`8539ba68b5157a156575d5164d1e5c46ad97cb88679a4e64235bfe5a2437953f`

---

## 12. Android Implementation Constants

```java
public class OxyraXConfig {
    // Network identity
    public static final String COIN_NAME = "Oxyra X";
    public static final String COIN_SYMBOL = "OXRX";
    public static final int DECIMALS = 12;
    
    // Network configuration
    public static final String RPC_URL = "https://monero.bad-abda.online/";
    public static final int P2P_PORT = 18080;
    public static final int RPC_PORT = 18081;
    
    // Genesis information
    public static final String GENESIS_HASH = 
        "8539ba68b5157a156575d5164d1e5c46ad97cb88679a4e64235bfe5a2437953f";
    public static final String GENESIS_TX = 
        "013c01ff00018080b0b1af8389d129020c0045d8c6ed4d609e90e19ec8d9e188b5cd03a4cf8bf94f126cc61796cb967f2101ad615f1095fd6f56e759a61fe1b73b73904ab4339869dd38211c27a08e4284d9";
    public static final int GENESIS_NONCE = 10000;
    
    // Network ID
    public static final byte[] NETWORK_ID = {
        (byte)0x65, (byte)0x45, (byte)0x33, (byte)0xED,
        (byte)0xF3, (byte)0x22, (byte)0x47, (byte)0xAB,
        (byte)0xBA, (byte)0xC8, (byte)0x94, (byte)0x5A,
        (byte)0xA8, (byte)0x31, (byte)0xEB, (byte)0x4F
    };
    
    // Address prefixes
    public static final int ADDRESS_PREFIX_STANDARD = 18;
    public static final int ADDRESS_PREFIX_INTEGRATED = 19;
    public static final int ADDRESS_PREFIX_SUBADDRESS = 42;
    
    // Timing
    public static final int BLOCK_TIME_SECONDS = 120;
    public static final int UNLOCK_BLOCKS = 60;
    
    // Fees (in atomic units)
    public static final long MIN_FEE = 2000000000L;  // 0.002 OXRX
    public static final long DUST_THRESHOLD = 2000000000L;
    
    // Privacy
    public static final int MIN_RING_SIZE = 16;
    
    // Seed nodes
    public static final String[] SEED_NODES = {
        "seeds.moneroseeds.se:18080",
        "seeds.moneroseeds.ae.org:18080",
        "seeds.moneroseeds.ch:18080",
        "seeds.moneroseeds.li:18080"
    };
}
```

---

## 13. Key Files Provided

1. **ANDROID_INTEGRATION_INFO.md** - Complete detailed documentation
2. **oxyrax_android_config.json** - JSON configuration file
3. **GET_GENESIS_HASH.md** - How to retrieve genesis hash
4. **ANDROID_TEAM_SUMMARY.md** - This quick reference (you are here)

---

## 14. Important Notes

### Privacy Features
- ✅ Ring Signatures (minimum 16)
- ✅ Stealth Addresses
- ✅ RingCT (Confidential Transactions)
- ✅ Bulletproof+ (smaller proofs)
- ✅ View Tags (faster scanning)

### Security Reminders
1. Always use HTTPS for RPC connections
2. Store private keys in Android Keystore
3. Never log private keys or mnemonics
4. Validate all addresses before sending
5. Use proper random number generation

### Recommended Reference
- **Monerujo** - Open-source Android Monero wallet
  - GitHub: https://github.com/m2049r/xmrwallet
  - Good reference for Monero-based Android implementation

---

## 15. Support & Resources

For technical questions:
- Check the detailed documentation in `ANDROID_INTEGRATION_INFO.md`
- Test RPC endpoint: `https://monero.bad-abda.online/`
- Monero RPC documentation: https://www.getmonero.org/resources/developer-guides/wallet-rpc.html

---

## Summary Checklist

- [x] RPC Endpoint URL
- [x] Network ID / Chain ID
- [x] Genesis Hash
- [x] Genesis Transaction
- [x] Address Prefixes
- [x] Seed Nodes
- [x] Port Configuration
- [x] Coin Parameters (symbol, decimals)
- [x] Fee Configuration
- [x] Block Time & Unlock Time
- [x] Wallet RPC Methods List
- [x] Sample Integration Code

**All information verified and ready for Android integration!**

---

**Last Updated:** October 13, 2025  
**Version:** 1.0  
**Status:** ✅ Production Ready

