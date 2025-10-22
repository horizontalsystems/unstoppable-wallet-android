package io.horizontalsystems.bankwallet.core.adapters

/**
 * Oxyra X (OXRX) Configuration Constants
 * Based on client-provided information
 */
object OxyraConfig {
    
    // Basic Information
    const val COIN_NAME = "Oxyra X"
    const val COIN_SYMBOL = "OXRX"
    const val DECIMALS = 12
    
    // Network Configuration - Linux Server (from @mehul_chaudahri)
    const val RPC_URL = "https://monero.bad-abda.online/"
    const val EXPLORER_URL = "https://explorer.oxyrax.io"
    const val P2P_PORT = 18080
    const val RPC_PORT = 18081
    const val ZMQ_PORT = 18082
    
    // Local Development Configuration
    const val LOCAL_DAEMON_URL = "192.168.31.217:18081" // Host IP for Android emulator
    const val LOCAL_WALLET_RPC_URL = "192.168.31.217:18082"
    const val USE_LOCAL_NODE = true // Set to true for local demo
    
    // Genesis Information
    const val GENESIS_HASH = "8539ba68b5157a156575d5164d1e5c46ad97cb88679a4e64235bfe5a2437953f"
    const val GENESIS_TX = "013c01ff00018080b0b1af8389d129020c0045d8c6ed4d609e90e19ec8d9e188b5cd03a4cf8bf94f126cc61796cb967f2101ad615f1095fd6f56e759a61fe1b73b73904ab4339869dd38211c27a08e4284d9"
    const val GENESIS_NONCE = 10000
    
    // Network ID (UUID)
    val NETWORK_ID = byteArrayOf(
        0x65.toByte(), 0x45.toByte(), 0x33.toByte(), 0xED.toByte(),
        0xF3.toByte(), 0x22.toByte(), 0x47.toByte(), 0xAB.toByte(),
        0xBA.toByte(), 0xC8.toByte(), 0x94.toByte(), 0x5A.toByte(),
        0xA8.toByte(), 0x31.toByte(), 0xEB.toByte(), 0x4F.toByte()
    )
    
    // Address Prefixes
    const val ADDRESS_PREFIX_STANDARD = 18
    const val ADDRESS_PREFIX_INTEGRATED = 19
    const val ADDRESS_PREFIX_SUBADDRESS = 42
    
    // Timing
    const val BLOCK_TIME_SECONDS = 120
    const val UNLOCK_BLOCKS = 60
    
    // Fees (in atomic units)
    const val MIN_FEE = 2_000_000_000L // 0.002 OXRX
    const val DUST_THRESHOLD = 2_000_000_000L
    const val FEE_PER_BYTE = 300_000L
    
    // Privacy
    const val MIN_RING_SIZE = 16
    
    // Supply
    const val TOTAL_SUPPLY = 3_000_000_000L // 3 billion OXRX
    const val TOTAL_ATOMIC_UNITS = 3_000_000_000_000_000_000L
    
    // Seed Nodes
    val SEED_NODES = arrayOf(
        "seeds.moneroseeds.se:18080",
        "seeds.moneroseeds.ae.org:18080",
        "seeds.moneroseeds.ch:18080",
        "seeds.moneroseeds.li:18080"
    )
    
    // Explorer URLs
    fun getTransactionUrl(txId: String): String = "$EXPLORER_URL/tx/$txId"
    fun getBlockUrl(height: Long): String = "$EXPLORER_URL/block/$height"
    
    // Utility Functions
    fun atomicUnitsToOxyra(atomicUnits: Long): Double {
        return atomicUnits.toDouble() / Math.pow(10.0, DECIMALS.toDouble())
    }
    
    fun oxyraToAtomicUnits(oxyra: Double): Long {
        return (oxyra * Math.pow(10.0, DECIMALS.toDouble())).toLong()
    }
    
    fun formatOxyraAmount(atomicUnits: Long): String {
        val oxyra = atomicUnitsToOxyra(atomicUnits)
        return String.format("%.12f OXRX", oxyra)
    }
}
