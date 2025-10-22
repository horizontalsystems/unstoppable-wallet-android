package io.horizontalsystems.bankwallet.core.adapters

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * OxyraTransactionCache - Local cache for Oxyra transactions
 * This provides Room DB-like functionality for transaction storage
 */
class OxyraTransactionCache {
    
    companion object {
        private const val TAG = "OXYRA_INTEGRATION"
        private const val LOCAL_TESTING_TAG = "LOCAL_TESTING"
    }
    
    private val _cachedTransactions = MutableStateFlow<List<OxyraTransactionInfo>>(emptyList())
    val cachedTransactions: StateFlow<List<OxyraTransactionInfo>> = _cachedTransactions.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()
    
    fun cacheTransactions(transactions: List<OxyraTransactionInfo>) {
        Log.d(TAG, "💾 Caching ${transactions.size} Oxyra transactions")
        Log.d(LOCAL_TESTING_TAG, "💾 LOCAL_TESTING - Caching ${transactions.size} Oxyra transactions")
        
        try {
            // TODO: Implement real Room DB storage
            // This would involve:
            // 1. Insert/Update transactions in Room database
            // 2. Handle transaction deduplication
            // 3. Maintain transaction order
            // 4. Handle confirmation updates
            
            _cachedTransactions.value = transactions.sortedByDescending { it.timestamp }
            _lastSyncTime.value = System.currentTimeMillis()
            
            Log.i(TAG, "✅ Successfully cached ${transactions.size} transactions")
            Log.i(LOCAL_TESTING_TAG, "✅ LOCAL_TESTING - Successfully cached ${transactions.size} transactions")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error caching transactions", e)
            Log.e(LOCAL_TESTING_TAG, "❌ LOCAL_TESTING - Error caching transactions", e)
        }
    }
    
    fun getCachedTransactions(): List<OxyraTransactionInfo> {
        Log.d(TAG, "📖 Retrieving cached transactions")
        Log.d(LOCAL_TESTING_TAG, "📖 LOCAL_TESTING - Retrieving cached transactions")
        
        return _cachedTransactions.value
    }
    
    fun updateTransactionConfirmations(txHash: String, confirmations: Int) {
        Log.d(TAG, "🔄 Updating confirmations for transaction: $txHash")
        Log.d(LOCAL_TESTING_TAG, "🔄 LOCAL_TESTING - Updating confirmations for transaction: $txHash")
        
        try {
            val currentTransactions = _cachedTransactions.value.toMutableList()
            val transactionIndex = currentTransactions.indexOfFirst { it.hash == txHash }
            
            if (transactionIndex != -1) {
                val updatedTransaction = currentTransactions[transactionIndex].copy(
                    confirmations = confirmations,
                    isPending = confirmations == 0
                )
                currentTransactions[transactionIndex] = updatedTransaction
                _cachedTransactions.value = currentTransactions
                
                Log.i(TAG, "✅ Updated confirmations for transaction: $txHash")
                Log.i(LOCAL_TESTING_TAG, "✅ LOCAL_TESTING - Updated confirmations for transaction: $txHash")
            } else {
                Log.w(TAG, "⚠️ Transaction not found in cache: $txHash")
                Log.w(LOCAL_TESTING_TAG, "⚠️ LOCAL_TESTING - Transaction not found in cache: $txHash")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating transaction confirmations", e)
            Log.e(LOCAL_TESTING_TAG, "❌ LOCAL_TESTING - Error updating transaction confirmations", e)
        }
    }
    
    fun clearCache() {
        Log.d(TAG, "🗑️ Clearing transaction cache")
        Log.d(LOCAL_TESTING_TAG, "🗑️ LOCAL_TESTING - Clearing transaction cache")
        
        _cachedTransactions.value = emptyList()
        _lastSyncTime.value = 0L
        
        Log.i(TAG, "✅ Transaction cache cleared")
        Log.i(LOCAL_TESTING_TAG, "✅ LOCAL_TESTING - Transaction cache cleared")
    }
    
    fun getCacheStats(): CacheStats {
        val transactions = _cachedTransactions.value
        val pendingCount = transactions.count { it.isPending }
        val confirmedCount = transactions.count { !it.isPending }
        
        return CacheStats(
            totalTransactions = transactions.size,
            pendingTransactions = pendingCount,
            confirmedTransactions = confirmedCount,
            lastSyncTime = _lastSyncTime.value
        )
    }
}

data class CacheStats(
    val totalTransactions: Int,
    val pendingTransactions: Int,
    val confirmedTransactions: Int,
    val lastSyncTime: Long
)

