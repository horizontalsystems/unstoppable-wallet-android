package io.horizontalsystems.bankwallet.core.adapters

import android.util.Log
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

/**
 * OxyraKitUnitTests - Unit tests for OxyraKit functionality
 * Tests RPC methods, key derivation, and transaction handling
 */
@RunWith(MockitoJUnitRunner::class)
class OxyraKitUnitTests {
    
    companion object {
        private const val TAG = "OXYRA_INTEGRATION"
        private const val LOCAL_TESTING_TAG = "LOCAL_TESTING"
    }
    
    @Mock
    private lateinit var mockContext: android.content.Context
    
    private lateinit var oxyraKit: OxyraKit
    private lateinit var testSeed: OxyraSeed
    private lateinit var testNode: OxyraNode
    
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        Log.d(TAG, "🧪 Setting up OxyraKit unit tests")
        Log.d(LOCAL_TESTING_TAG, "🧪 LOCAL_TESTING - Setting up OxyraKit unit tests")
        
        // Create test seed
        testSeed = OxyraSeed.fromMnemonic("test mnemonic seed phrase for oxyra wallet development testing purposes only")
        
        // Create test node
        testNode = OxyraNode("192.168.31.217:18081", true)
        
        // Create OxyraKit instance
        oxyraKit = OxyraKit.getInstance(
            context = mockContext,
            seed = testSeed,
            birthdayHeightOrDate = "0",
            walletId = "test_wallet",
            node = testNode
        )
    }
    
    @Test
    fun testKeyDerivationFromMnemonic() {
        Log.d(TAG, "🔑 Testing key derivation from mnemonic")
        Log.d(LOCAL_TESTING_TAG, "🔑 LOCAL_TESTING - Testing key derivation from mnemonic")
        
        val mnemonic = "test mnemonic seed phrase for oxyra wallet development testing purposes only"
        val seed = OxyraSeed.fromMnemonic(mnemonic)
        
        // Verify seed properties
        assertNotNull("Seed should not be null", seed)
        assertEquals("Mnemonic should match", mnemonic, seed.mnemonic)
        assertTrue("Private spend key should not be empty", seed.privateSpendKey.isNotEmpty())
        assertTrue("Private view key should not be empty", seed.privateViewKey.isNotEmpty())
        assertTrue("Public spend key should not be empty", seed.publicSpendKey.isNotEmpty())
        assertTrue("Public view key should not be empty", seed.publicViewKey.isNotEmpty())
        
        Log.i(TAG, "✅ Key derivation test passed")
        Log.i(LOCAL_TESTING_TAG, "✅ LOCAL_TESTING - Key derivation test passed")
    }
    
    @Test
    fun testDeterministicKeyGeneration() {
        Log.d(TAG, "🔐 Testing deterministic key generation")
        Log.d(LOCAL_TESTING_TAG, "🔐 LOCAL_TESTING - Testing deterministic key generation")
        
        val mnemonic = "test mnemonic seed phrase for oxyra wallet development testing purposes only"
        
        // Generate keys twice with same mnemonic
        val seed1 = OxyraSeed.fromMnemonic(mnemonic)
        val seed2 = OxyraSeed.fromMnemonic(mnemonic)
        
        // Keys should be deterministic (same mnemonic = same keys)
        assertEquals("Private spend keys should be identical", seed1.privateSpendKey, seed2.privateSpendKey)
        assertEquals("Private view keys should be identical", seed1.privateViewKey, seed2.privateViewKey)
        assertEquals("Public spend keys should be identical", seed1.publicSpendKey, seed2.publicSpendKey)
        assertEquals("Public view keys should be identical", seed1.publicViewKey, seed2.publicViewKey)
        
        Log.i(TAG, "✅ Deterministic key generation test passed")
        Log.i(LOCAL_TESTING_TAG, "✅ LOCAL_TESTING - Deterministic key generation test passed")
    }
    
    @Test
    fun testAddressGeneration() = runBlocking {
        Log.d(TAG, "🏠 Testing address generation")
        Log.d(LOCAL_TESTING_TAG, "🏠 LOCAL_TESTING - Testing address generation")
        
        val address = oxyraKit.receiveAddress
        
        // Verify address properties
        assertNotNull("Address should not be null", address)
        assertTrue("Address should not be empty", address.isNotEmpty())
        assertTrue("Address should start with Oxyra", address.startsWith("Oxyra"))
        
        Log.i(TAG, "✅ Address generation test passed: $address")
        Log.i(LOCAL_TESTING_TAG, "✅ LOCAL_TESTING - Address generation test passed: $address")
    }
    
    @Test
    fun testTransactionCache() = runBlocking {
        Log.d(TAG, "💾 Testing transaction cache")
        Log.d(LOCAL_TESTING_TAG, "💾 LOCAL_TESTING - Testing transaction cache")
        
        val cache = OxyraTransactionCache()
        
        // Test empty cache
        val emptyTransactions = cache.getCachedTransactions()
        assertEquals("Empty cache should return empty list", 0, emptyTransactions.size)
        
        // Test cache stats
        val emptyStats = cache.getCacheStats()
        assertEquals("Empty cache stats should be zero", 0, emptyStats.totalTransactions)
        assertEquals("Empty cache stats should be zero", 0, emptyStats.pendingTransactions)
        assertEquals("Empty cache stats should be zero", 0, emptyStats.confirmedTransactions)
        
        // Test caching transactions
        val mockTransactions = listOf(
            OxyraTransactionInfo(
                hash = "test_tx_1",
                amount = 1000000L,
                fee = 100000L,
                timestamp = System.currentTimeMillis(),
                blockheight = 1000L,
                confirmations = 1,
                isPending = false,
                isFailed = false,
                direction = OxyraTransactionInfo.Direction.Direction_In,
                accountIndex = 0,
                addressIndex = 0,
                notes = "Test transaction"
            )
        )
        
        cache.cacheTransactions(mockTransactions)
        val cachedTransactions = cache.getCachedTransactions()
        assertEquals("Cached transactions should match", 1, cachedTransactions.size)
        assertEquals("Cached transaction hash should match", "test_tx_1", cachedTransactions[0].hash)
        
        // Test cache stats after caching
        val stats = cache.getCacheStats()
        assertEquals("Cache stats should show 1 transaction", 1, stats.totalTransactions)
        assertEquals("Cache stats should show 0 pending", 0, stats.pendingTransactions)
        assertEquals("Cache stats should show 1 confirmed", 1, stats.confirmedTransactions)
        
        Log.i(TAG, "✅ Transaction cache test passed")
        Log.i(LOCAL_TESTING_TAG, "✅ LOCAL_TESTING - Transaction cache test passed")
    }
    
    @Test
    fun testTransactionConfirmationUpdate() = runBlocking {
        Log.d(TAG, "🔄 Testing transaction confirmation update")
        Log.d(LOCAL_TESTING_TAG, "🔄 LOCAL_TESTING - Testing transaction confirmation update")
        
        val cache = OxyraTransactionCache()
        
        // Add a pending transaction
        val pendingTransaction = OxyraTransactionInfo(
            hash = "test_tx_pending",
            amount = 1000000L,
            fee = 100000L,
            timestamp = System.currentTimeMillis(),
            blockheight = 0L,
            confirmations = 0,
            isPending = true,
            isFailed = false,
            direction = OxyraTransactionInfo.Direction.Direction_In,
            accountIndex = 0,
            addressIndex = 0,
            notes = "Pending transaction"
        )
        
        cache.cacheTransactions(listOf(pendingTransaction))
        
        // Update confirmations
        cache.updateTransactionConfirmations("test_tx_pending", 5)
        
        val updatedTransactions = cache.getCachedTransactions()
        assertEquals("Should have 1 transaction", 1, updatedTransactions.size)
        assertEquals("Confirmations should be updated", 5, updatedTransactions[0].confirmations)
        assertFalse("Transaction should not be pending", updatedTransactions[0].isPending)
        
        Log.i(TAG, "✅ Transaction confirmation update test passed")
        Log.i(LOCAL_TESTING_TAG, "✅ LOCAL_TESTING - Transaction confirmation update test passed")
    }
    
    @Test
    fun testCacheClear() = runBlocking {
        Log.d(TAG, "🗑️ Testing cache clear")
        Log.d(LOCAL_TESTING_TAG, "🗑️ LOCAL_TESTING - Testing cache clear")
        
        val cache = OxyraTransactionCache()
        
        // Add some transactions
        val mockTransactions = listOf(
            OxyraTransactionInfo(
                hash = "test_tx_1",
                amount = 1000000L,
                fee = 100000L,
                timestamp = System.currentTimeMillis(),
                blockheight = 1000L,
                confirmations = 1,
                isPending = false,
                isFailed = false,
                direction = OxyraTransactionInfo.Direction.Direction_In,
                accountIndex = 0,
                addressIndex = 0,
                notes = "Test transaction 1"
            ),
            OxyraTransactionInfo(
                hash = "test_tx_2",
                amount = 2000000L,
                fee = 200000L,
                timestamp = System.currentTimeMillis(),
                blockheight = 1001L,
                confirmations = 2,
                isPending = false,
                isFailed = false,
                direction = OxyraTransactionInfo.Direction.Direction_Out,
                accountIndex = 0,
                addressIndex = 0,
                notes = "Test transaction 2"
            )
        )
        
        cache.cacheTransactions(mockTransactions)
        assertEquals("Should have 2 transactions", 2, cache.getCachedTransactions().size)
        
        // Clear cache
        cache.clearCache()
        
        // Verify cache is empty
        assertEquals("Cache should be empty after clear", 0, cache.getCachedTransactions().size)
        
        val stats = cache.getCacheStats()
        assertEquals("Cache stats should be zero after clear", 0, stats.totalTransactions)
        assertEquals("Last sync time should be zero after clear", 0L, stats.lastSyncTime)
        
        Log.i(TAG, "✅ Cache clear test passed")
        Log.i(LOCAL_TESTING_TAG, "✅ LOCAL_TESTING - Cache clear test passed")
    }
    
    @Test
    fun testOxyraSeedGeneration() {
        Log.d(TAG, "🌱 Testing OxyraSeed generation")
        Log.d(LOCAL_TESTING_TAG, "🌱 LOCAL_TESTING - Testing OxyraSeed generation")
        
        val newSeed = OxyraSeed.generateNew()
        
        // Verify generated seed properties
        assertNotNull("Generated seed should not be null", newSeed)
        assertTrue("Generated mnemonic should not be empty", newSeed.mnemonic.isNotEmpty())
        assertTrue("Generated private spend key should not be empty", newSeed.privateSpendKey.isNotEmpty())
        assertTrue("Generated private view key should not be empty", newSeed.privateViewKey.isNotEmpty())
        assertTrue("Generated public spend key should not be empty", newSeed.publicSpendKey.isNotEmpty())
        assertTrue("Generated public view key should not be empty", newSeed.publicViewKey.isNotEmpty())
        
        Log.i(TAG, "✅ OxyraSeed generation test passed")
        Log.i(LOCAL_TESTING_TAG, "✅ LOCAL_TESTING - OxyraSeed generation test passed")
    }
}

