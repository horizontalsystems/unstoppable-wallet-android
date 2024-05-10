package io.horizontalsystems.bankwallet.core.storage.migrations

import android.os.Parcelable
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.parcelize.Parcelize

object Migration_45_46 : Migration(45, 46) {
    override fun migrate(db: SupportSQLiteDatabase) {
        renameColumnEnabledWallet(db)
        renameColumnEnabledWalletCache(db)
        renameColumnRestoreSettingRecord(db)
        renameColumnNftCollectionRecord(db)
        renameColumnNftAssetRecord(db)
        deleteRecordsAppLogMemory(db)
    }

    private fun renameColumnEnabledWallet(database: SupportSQLiteDatabase) {
        val walletsCursor = database.query("SELECT * FROM EnabledWallet")
        val map = mutableMapOf<String, String>()
        while (walletsCursor.moveToNext()) {
            val coinIdColumnIndex = walletsCursor.getColumnIndex("coinId")
            if (coinIdColumnIndex >= 0) {
                val coinId = walletsCursor.getString(coinIdColumnIndex)
                if (map.containsKey(coinId)) continue
                map[coinId] = getTokenQueryForCoinId(coinId).id
            }
        }
        map.forEach { (coinId, tokenQueryId) ->
            database.execSQL("UPDATE EnabledWallet SET `coinId` = '$tokenQueryId' WHERE `coinId` = '$coinId'")
        }

        database.execSQL("ALTER TABLE EnabledWallet RENAME TO TempEnabledWallet")
        database.execSQL("CREATE TABLE IF NOT EXISTS `EnabledWallet` (`tokenQueryId` TEXT NOT NULL, `coinSettingsId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `walletOrder` INTEGER, `coinName` TEXT, `coinCode` TEXT, `coinDecimals` INTEGER, PRIMARY KEY(`tokenQueryId`, `coinSettingsId`, `accountId`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
        database.execSQL("INSERT INTO EnabledWallet (`tokenQueryId`, `coinSettingsId`, `accountId`, `walletOrder`, `coinName`, `coinCode`, `coinDecimals`) SELECT `coinId`,`coinSettingsId`,`accountId`,`walletOrder`,`coinName`,`coinCode`,`coinDecimals` FROM TempEnabledWallet")
        database.execSQL("DROP TABLE IF EXISTS TempEnabledWallet")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_EnabledWallet_accountId` ON `EnabledWallet` (`accountId`)")
    }

    private fun getTokenQueryForCoinId(coinId: String) =
        when (val coinType = CoinType.fromId(coinId)) {
            is CoinType.Bitcoin -> {
                TokenQuery(BlockchainType.Bitcoin, TokenType.Native)
            }
            is CoinType.BitcoinCash -> {
                TokenQuery(BlockchainType.BitcoinCash, TokenType.Native)
            }
            is CoinType.Litecoin -> {
                TokenQuery(BlockchainType.Litecoin, TokenType.Native)
            }
            is CoinType.Dash -> {
                TokenQuery(BlockchainType.Dash, TokenType.Native)
            }
            is CoinType.Zcash -> {
                TokenQuery(BlockchainType.Zcash, TokenType.Native)
            }
            is CoinType.Ethereum -> {
                TokenQuery(BlockchainType.Ethereum, TokenType.Native)
            }
            is CoinType.BinanceSmartChain -> {
                TokenQuery(BlockchainType.BinanceSmartChain, TokenType.Native)
            }
            is CoinType.Polygon -> {
                TokenQuery(BlockchainType.Polygon, TokenType.Native)
            }
            is CoinType.EthereumOptimism -> {
                TokenQuery(BlockchainType.Optimism, TokenType.Native)
            }
            is CoinType.EthereumArbitrumOne -> {
                TokenQuery(BlockchainType.ArbitrumOne, TokenType.Native)
            }
            is CoinType.Erc20 -> {
                TokenQuery(BlockchainType.Ethereum, TokenType.Eip20(coinType.address))
            }
            is CoinType.Bep20 -> {
                TokenQuery(BlockchainType.BinanceSmartChain, TokenType.Eip20(coinType.address))
            }
            is CoinType.Mrc20 -> {
                TokenQuery(BlockchainType.Polygon, TokenType.Eip20(coinType.address))
            }
            is CoinType.OptimismErc20 -> {
                TokenQuery(BlockchainType.Optimism, TokenType.Eip20(coinType.address))
            }
            is CoinType.ArbitrumOneErc20 -> {
                TokenQuery(BlockchainType.ArbitrumOne, TokenType.Eip20(coinType.address))
            }
            is CoinType.Bep2 -> if (coinType.symbol == "BNB") {
                TokenQuery(BlockchainType.BinanceChain, TokenType.Native)
            } else {
                TokenQuery(BlockchainType.BinanceChain, TokenType.Bep2(coinType.symbol))
            }
            else -> {
                TokenQuery(BlockchainType.Unsupported(""), TokenType.Unsupported("", ""))
            }
        }

    private fun renameColumnEnabledWalletCache(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS EnabledWalletCache")
        database.execSQL("CREATE TABLE IF NOT EXISTS `EnabledWalletCache` (`tokenQueryId` TEXT NOT NULL, `coinSettingsId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `balance` TEXT NOT NULL, `balanceLocked` TEXT NOT NULL, PRIMARY KEY(`tokenQueryId`, `coinSettingsId`, `accountId`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
    }

    private fun renameColumnRestoreSettingRecord(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE RestoreSettingRecord RENAME TO TempRestoreSettingRecord")
        database.execSQL("CREATE TABLE IF NOT EXISTS `RestoreSettingRecord` (`accountId` TEXT NOT NULL, `blockchainTypeUid` TEXT NOT NULL, `key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`accountId`, `blockchainTypeUid`, `key`))")
        database.execSQL("INSERT INTO RestoreSettingRecord (`accountId`,`blockchainTypeUid`,`key`,`value`) SELECT `accountId`,`coinId`,`key`,`value` FROM TempRestoreSettingRecord")
        database.execSQL("DROP TABLE IF EXISTS TempRestoreSettingRecord")
    }

    private fun renameColumnNftCollectionRecord(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS NftCollectionRecord")
        database.execSQL("CREATE TABLE IF NOT EXISTS `NftCollectionRecord` (`accountId` TEXT NOT NULL, `uid` TEXT NOT NULL, `name` TEXT NOT NULL, `imageUrl` TEXT, `totalSupply` INTEGER NOT NULL, `averagePrice7d_tokenQueryId` TEXT, `averagePrice7d_value` TEXT, `averagePrice30d_tokenQueryId` TEXT, `averagePrice30d_value` TEXT, `floorPrice_tokenQueryId` TEXT, `floorPrice_value` TEXT, `external_url` TEXT, `discord_url` TEXT, `twitter_username` TEXT, PRIMARY KEY(`accountId`, `uid`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
    }

    private fun renameColumnNftAssetRecord(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS NftAssetRecord")
        database.execSQL("CREATE TABLE IF NOT EXISTS `NftAssetRecord` (`accountId` TEXT NOT NULL, `collectionUid` TEXT NOT NULL, `tokenId` TEXT NOT NULL, `name` TEXT, `imageUrl` TEXT, `imagePreviewUrl` TEXT, `description` TEXT, `onSale` INTEGER NOT NULL, `attributes` TEXT NOT NULL, `tokenQueryId` TEXT, `value` TEXT, `contract_address` TEXT NOT NULL, `contract_type` TEXT NOT NULL, `external_link` TEXT, `permalink` TEXT, PRIMARY KEY(`accountId`, `tokenId`, `contract_address`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
    }

    private fun deleteRecordsAppLogMemory(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM `LogEntry` WHERE `actionId` = 'low memory'")
    }
}

private sealed class CoinType : Parcelable {
    @Parcelize
    object Bitcoin : CoinType()

    @Parcelize
    object BitcoinCash : CoinType()

    @Parcelize
    object Litecoin : CoinType()

    @Parcelize
    object Dash : CoinType()

    @Parcelize
    object Zcash : CoinType()

    @Parcelize
    object Ethereum : CoinType()

    @Parcelize
    object BinanceSmartChain : CoinType()

    @Parcelize
    object Polygon : CoinType()

    @Parcelize
    object EthereumOptimism : CoinType()

    @Parcelize
    object EthereumArbitrumOne : CoinType()

    @Parcelize
    class Erc20(val address: String) : CoinType()

    @Parcelize
    class Bep20(val address: String) : CoinType()

    @Parcelize
    class Mrc20(val address: String) : CoinType()

    @Parcelize
    class OptimismErc20(val address: String) : CoinType()

    @Parcelize
    class ArbitrumOneErc20(val address: String) : CoinType()

    @Parcelize
    class Bep2(val symbol: String) : CoinType()

    @Parcelize
    class Avalanche(val address: String) : CoinType()

    @Parcelize
    class Fantom(val address: String) : CoinType()

    @Parcelize
    class HarmonyShard0(val address: String) : CoinType()

    @Parcelize
    class HuobiToken(val address: String) : CoinType()

    @Parcelize
    class Iotex(val address: String) : CoinType()

    @Parcelize
    class Moonriver(val address: String) : CoinType()

    @Parcelize
    class OkexChain(val address: String) : CoinType()

    @Parcelize
    class Solana(val address: String) : CoinType()

    @Parcelize
    class Sora(val address: String) : CoinType()

    @Parcelize
    class Tomochain(val address: String) : CoinType()

    @Parcelize
    class Xdai(val address: String) : CoinType()

    @Parcelize
    class Unsupported(val type: String) : CoinType()

    val id: String
        get() = when (this) {
            is Bitcoin -> "bitcoin"
            is BitcoinCash -> "bitcoinCash"
            is Litecoin -> "litecoin"
            is Dash -> "dash"
            is Zcash -> "zcash"
            is Ethereum -> "ethereum"
            is BinanceSmartChain -> "binanceSmartChain"
            is Polygon -> "polygon"
            is EthereumOptimism -> "ethereumOptimism"
            is EthereumArbitrumOne -> "ethereumArbitrumOne"
            is Erc20 -> "erc20|$address"
            is Bep20 -> "bep20|$address"
            is Mrc20 -> "mrc20|$address"
            is OptimismErc20 -> "optimismErc20|$address"
            is ArbitrumOneErc20 -> "arbitrumOneErc20|$address"
            is Bep2 -> "bep2|$symbol"
            is Avalanche -> "avalanche|$address"
            is Fantom -> "fantom|$address"
            is HarmonyShard0 -> "harmonyShard0|$address"
            is HuobiToken -> "huobiToken|$address"
            is Iotex -> "iotex|$address"
            is Moonriver -> "moonriver|$address"
            is OkexChain -> "okexChain|$address"
            is Solana -> "solana|$address"
            is Sora -> "sora|$address"
            is Tomochain -> "tomochain|$address"
            is Xdai -> "xdai|$address"
            is Unsupported -> "unsupported|$type"
        }

    override fun equals(other: Any?): Boolean {
        return other is CoinType && other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString() = when (this) {
        Bitcoin -> "bitcoin"
        BitcoinCash -> "bitcoinCash"
        Litecoin -> "litecoin"
        Dash -> "dash"
        Zcash -> "zcash"
        Ethereum -> "ethereum"
        BinanceSmartChain -> "binanceSmartChain"
        Polygon -> "polygon"
        EthereumOptimism -> "ethereumOptimism"
        EthereumArbitrumOne -> "ethereumArbitrumOne"
        is Erc20 -> shorted("erc20", address)
        is Bep20 -> shorted("bep20", address)
        is Mrc20 -> shorted("mrc20", address)
        is OptimismErc20 -> shorted("optimismErc20", address)
        is ArbitrumOneErc20 -> shorted("arbitrumOneErc20", address)
        is Bep2 -> "bep2|$symbol"
        is Avalanche -> shorted("avalanche", address)
        is Fantom -> shorted("fantom", address)
        is HarmonyShard0 -> shorted("harmonyShard0", address)
        is HuobiToken -> shorted("huobiToken", address)
        is Iotex -> shorted("iotex", address)
        is Moonriver -> shorted("moonriver", address)
        is OkexChain -> shorted("okexChain", address)
        is Solana -> shorted("solana", address)
        is Sora -> shorted("sora", address)
        is Tomochain -> shorted("tomochain", address)
        is Xdai -> shorted("xdai", address)
        is Unsupported -> "unsupported|$type"
    }

    private fun shorted(prefix: String, address: String): String {
        return "$prefix|${address.take(4)}...${address.takeLast(2)}"
    }

    companion object {
        fun fromId(id: String): CoinType {
            val chunks = id.split("|")

            return if (chunks.size == 1) {
                when (chunks[0]) {
                    "bitcoin" -> Bitcoin
                    "bitcoinCash" -> BitcoinCash
                    "litecoin" -> Litecoin
                    "dash" -> Dash
                    "zcash" -> Zcash
                    "ethereum" -> Ethereum
                    "binanceSmartChain" -> BinanceSmartChain
                    "polygon" -> Polygon
                    "ethereumOptimism" -> EthereumOptimism
                    "ethereumArbitrumOne" -> EthereumArbitrumOne
                    else -> Unsupported(chunks[0])
                }
            } else {
                when (chunks[0]) {
                    "erc20" -> Erc20(chunks[1])
                    "bep2" -> Bep2(chunks[1])
                    "bep20" -> Bep20(chunks[1])
                    "mrc20" -> Mrc20(chunks[1])
                    "optimismErc20" -> OptimismErc20(chunks[1])
                    "arbitrumOneErc20" -> ArbitrumOneErc20(chunks[1])
                    "avalanche" -> Avalanche(chunks[1])
                    "fantom" -> Fantom(chunks[1])
                    "harmony-shard-0" -> HarmonyShard0(chunks[1])
                    "huobi-token" -> HuobiToken(chunks[1])
                    "iotex" -> Iotex(chunks[1])
                    "moonriver" -> Moonriver(chunks[1])
                    "okex-chain" -> OkexChain(chunks[1])
                    "solana" -> Solana(chunks[1])
                    "sora" -> Sora(chunks[1])
                    "tomochain" -> Tomochain(chunks[1])
                    "xdai" -> Xdai(chunks[1])
                    "unsupported" -> Unsupported(chunks.drop(1).joinToString("|"))
                    else -> Unsupported(chunks.joinToString("|"))
                }
            }
        }
    }

}
