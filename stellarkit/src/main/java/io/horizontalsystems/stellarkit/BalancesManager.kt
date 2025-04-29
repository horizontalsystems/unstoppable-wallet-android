package io.horizontalsystems.stellarkit

import android.util.Log
import io.horizontalsystems.stellarkit.room.AssetBalance
import io.horizontalsystems.stellarkit.room.AssetNativeBalance
import io.horizontalsystems.stellarkit.room.BalanceDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.stellar.sdk.Server
import java.math.BigDecimal

class BalancesManager(
    private val server: Server,
    private val balanceDao: BalanceDao,
    private val accountId: String
) {
    private val _syncStateFlow = MutableStateFlow<SyncState>(SyncState.NotSynced(StellarKit.SyncError.NotStarted))
    val syncStateFlow = _syncStateFlow.asStateFlow()

    private val _xlmBalanceFlow = MutableStateFlow(balanceDao.getNativeBalance()?.balance ?: BigDecimal.ZERO)
    val xlmBalanceFlow = _xlmBalanceFlow.asStateFlow()

    private val _assetBalanceMapFlow = MutableStateFlow(getInitialAssetBalanceMap())
    val assetBalanceMapFlow = _assetBalanceMapFlow.asStateFlow()

    fun getInitialAssetBalanceMap(): Map<String, BigDecimal> {
        val assetBalances = balanceDao.getAssetBalances()

        return assetBalances.map {
            "${it.code}:${it.issuer}" to it.balance
        }.toMap()
    }

    fun sync() {
        Log.d("AAA", "Syncing balances...")

        if (_syncStateFlow.value is SyncState.Syncing) {
            Log.d("AAA","Syncing balances is in progress")
            return
        }

        _syncStateFlow.update {
            SyncState.Syncing
        }

        try {
            val accounts = server.accounts()
            val account = accounts.account(accountId)

            val assetBalances = mutableListOf<AssetBalance>()

            account.balances.forEach { balance ->
                val balanceBigDecimal = balance.balance.toBigDecimal()
                if (balance.assetType == "native") {
                    updateXlmBalance(balanceBigDecimal)
                } else {
                    assetBalances.add(
                        AssetBalance(
                            type = balance.assetType,
                            code = balance.assetCode,
                            issuer = balance.assetIssuer,
                            balance = balanceBigDecimal,
                        )
                    )
                }
            }

            if (assetBalances.isNotEmpty()) {
                balanceDao.insertAll(assetBalances)

                _assetBalanceMapFlow.update {
                    assetBalances.map {
                        "${it.code}:${it.issuer}" to it.balance
                    }.toMap()
                }
            }

            _syncStateFlow.update {
                SyncState.Synced
            }

        } catch (e: Throwable) {
            _syncStateFlow.update {
                SyncState.NotSynced(e)
            }
        }
    }

    private fun updateXlmBalance(amount: BigDecimal) {
        _xlmBalanceFlow.update { amount }
        balanceDao.insertNative(
            AssetNativeBalance(balance = amount)
        )
    }
}
