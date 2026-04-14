package com.quantum.wallet.bankwallet.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_31_32
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_32_33
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_33_34
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_34_35
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_35_36
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_36_37
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_37_38
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_38_39
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_39_40
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_40_41
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_41_42
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_42_43
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_43_44
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_44_45
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_45_46
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_46_47
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_47_48
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_48_49
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_49_50
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_50_51
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_51_52
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_52_53
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_53_54
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_54_55
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_55_56
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_56_57
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_57_58
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_58_59
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_59_60
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_60_61
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_61_62
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_62_63
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_63_64
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_64_65
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_65_66
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_66_67
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_67_68
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_68_69
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_69_70
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_70_71
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_71_72
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_72_73
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_73_74
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_74_75
import com.quantum.wallet.bankwallet.core.storage.migrations.Migration_75_76
import com.quantum.wallet.bankwallet.entities.ActiveAccount
import com.quantum.wallet.bankwallet.entities.BlockchainSettingRecord
import com.quantum.wallet.bankwallet.entities.EnabledWallet
import com.quantum.wallet.bankwallet.entities.EnabledWalletCache
import com.quantum.wallet.bankwallet.entities.EvmAddressLabel
import com.quantum.wallet.bankwallet.entities.EvmMethodLabel
import com.quantum.wallet.bankwallet.entities.EvmSyncSourceRecord
import com.quantum.wallet.bankwallet.entities.LogEntry
import com.quantum.wallet.bankwallet.entities.MoneroNodeRecord
import com.quantum.wallet.bankwallet.entities.RecentAddress
import com.quantum.wallet.bankwallet.entities.RestoreSettingRecord
import com.quantum.wallet.bankwallet.entities.ScannedTransaction
import com.quantum.wallet.bankwallet.entities.SpamScanState
import com.quantum.wallet.bankwallet.entities.StatRecord
import com.quantum.wallet.bankwallet.entities.SwapProviderAssetRecord
import com.quantum.wallet.bankwallet.entities.SwapRecord
import com.quantum.wallet.bankwallet.entities.SyncerState
import com.quantum.wallet.bankwallet.entities.TokenAutoEnabledBlockchain
import com.quantum.wallet.bankwallet.entities.nft.NftAssetBriefMetadataRecord
import com.quantum.wallet.bankwallet.entities.nft.NftAssetRecord
import com.quantum.wallet.bankwallet.entities.nft.NftCollectionRecord
import com.quantum.wallet.bankwallet.entities.nft.NftMetadataSyncRecord
import com.quantum.wallet.bankwallet.modules.chart.ChartIndicatorSetting
import com.quantum.wallet.bankwallet.modules.chart.ChartIndicatorSettingsDao
import com.quantum.wallet.bankwallet.modules.pin.core.Pin
import com.quantum.wallet.bankwallet.modules.pin.core.PinDao
import com.quantum.wallet.bankwallet.modules.profeatures.storage.ProFeaturesDao
import com.quantum.wallet.bankwallet.modules.profeatures.storage.ProFeaturesSessionKey
import com.quantum.wallet.bankwallet.modules.walletconnect.storage.WCSessionDao
import com.quantum.wallet.bankwallet.modules.walletconnect.storage.WalletConnectV2Session

@Database(version = 76, exportSchema = false, entities = [
    EnabledWallet::class,
    EnabledWalletCache::class,
    AccountRecord::class,
    BlockchainSettingRecord::class,
    EvmSyncSourceRecord::class,
    LogEntry::class,
    FavoriteCoin::class,
    WalletConnectV2Session::class,
    RestoreSettingRecord::class,
    ActiveAccount::class,
    NftCollectionRecord::class,
    NftAssetRecord::class,
    NftMetadataSyncRecord::class,
    NftAssetBriefMetadataRecord::class,
    ProFeaturesSessionKey::class,
    EvmAddressLabel::class,
    EvmMethodLabel::class,
    SyncerState::class,
    TokenAutoEnabledBlockchain::class,
    ChartIndicatorSetting::class,
    Pin::class,
    StatRecord::class,
    ScannedTransaction::class,
    SpamScanState::class,
    RecentAddress::class,
    MoneroNodeRecord::class,
    SwapProviderAssetRecord::class,
    SwapRecord::class,
])

@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chartIndicatorSettingsDao(): ChartIndicatorSettingsDao
    abstract fun walletsDao(): EnabledWalletsDao
    abstract fun enabledWalletsCacheDao(): EnabledWalletsCacheDao
    abstract fun accountsDao(): AccountsDao
    abstract fun blockchainSettingDao(): BlockchainSettingDao
    abstract fun evmSyncSourceDao(): EvmSyncSourceDao
    abstract fun restoreSettingDao(): RestoreSettingDao
    abstract fun logsDao(): LogsDao
    abstract fun marketFavoritesDao(): MarketFavoritesDao
    abstract fun wcSessionDao(): WCSessionDao
    abstract fun nftDao(): NftDao
    abstract fun proFeaturesDao(): ProFeaturesDao
    abstract fun evmAddressLabelDao(): EvmAddressLabelDao
    abstract fun evmMethodLabelDao(): EvmMethodLabelDao
    abstract fun syncerStateDao(): SyncerStateDao
    abstract fun tokenAutoEnabledBlockchainDao(): TokenAutoEnabledBlockchainDao
    abstract fun pinDao(): PinDao
    abstract fun statsDao(): StatsDao
    abstract fun scannedTransactionDao(): ScannedTransactionDao
    abstract fun recentAddressDao(): RecentAddressDao
    abstract fun moneroNodeDao(): MoneroNodeDao
    abstract fun swapProviderAssetDao(): SwapProviderAssetDao
    abstract fun swapRecordDao(): SwapRecordDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "dbBankWallet")
//                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .addMigrations(
                            Migration_31_32,
                            Migration_32_33,
                            Migration_33_34,
                            Migration_34_35,
                            Migration_35_36,
                            Migration_36_37,
                            Migration_37_38,
                            Migration_38_39,
                            Migration_39_40,
                            Migration_40_41,
                            Migration_41_42,
                            Migration_42_43,
                            Migration_43_44,
                            Migration_44_45,
                            Migration_45_46,
                            Migration_46_47,
                            Migration_47_48,
                            Migration_48_49,
                            Migration_49_50,
                            Migration_50_51,
                            Migration_51_52,
                            Migration_52_53,
                            Migration_53_54,
                            Migration_54_55,
                            Migration_55_56,
                            Migration_56_57,
                            Migration_57_58,
                            Migration_58_59,
                            Migration_59_60,
                            Migration_60_61,
                            Migration_61_62,
                            Migration_62_63,
                            Migration_63_64,
                            Migration_64_65,
                            Migration_65_66,
                            Migration_66_67,
                            Migration_67_68,
                            Migration_68_69,
                            Migration_69_70,
                            Migration_70_71,
                            Migration_71_72,
                            Migration_72_73,
                            Migration_73_74,
                            Migration_74_75,
                    Migration_75_76,
                    )
                    .build()
        }

    }
}
