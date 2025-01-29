package cash.p.terminal.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cash.p.terminal.core.providers.CexAssetRaw
import cash.p.terminal.core.storage.migrations.Migration_31_32
import cash.p.terminal.core.storage.migrations.Migration_32_33
import cash.p.terminal.core.storage.migrations.Migration_33_34
import cash.p.terminal.core.storage.migrations.Migration_34_35
import cash.p.terminal.core.storage.migrations.Migration_35_36
import cash.p.terminal.core.storage.migrations.Migration_36_37
import cash.p.terminal.core.storage.migrations.Migration_37_38
import cash.p.terminal.core.storage.migrations.Migration_38_39
import cash.p.terminal.core.storage.migrations.Migration_39_40
import cash.p.terminal.core.storage.migrations.Migration_40_41
import cash.p.terminal.core.storage.migrations.Migration_41_42
import cash.p.terminal.core.storage.migrations.Migration_42_43
import cash.p.terminal.core.storage.migrations.Migration_43_44
import cash.p.terminal.core.storage.migrations.Migration_44_45
import cash.p.terminal.core.storage.migrations.Migration_45_46
import cash.p.terminal.core.storage.migrations.Migration_46_47
import cash.p.terminal.core.storage.migrations.Migration_47_48
import cash.p.terminal.core.storage.migrations.Migration_48_49
import cash.p.terminal.core.storage.migrations.Migration_49_50
import cash.p.terminal.core.storage.migrations.Migration_50_51
import cash.p.terminal.core.storage.migrations.Migration_51_52
import cash.p.terminal.core.storage.migrations.Migration_52_53
import cash.p.terminal.core.storage.migrations.Migration_53_54
import cash.p.terminal.core.storage.migrations.Migration_54_55
import cash.p.terminal.core.storage.migrations.Migration_55_56
import cash.p.terminal.core.storage.migrations.Migration_56_57
import cash.p.terminal.core.storage.migrations.Migration_57_58
import cash.p.terminal.core.storage.migrations.Migration_58_59
import cash.p.terminal.core.storage.migrations.Migration_59_60
import cash.p.terminal.core.storage.migrations.Migration_60_61
import cash.p.terminal.core.storage.migrations.Migration_61_62
import cash.p.terminal.entities.ActiveAccount
import cash.p.terminal.entities.BlockchainSettingRecord
import cash.p.terminal.entities.ChangeNowTransaction
import cash.p.terminal.entities.EnabledWalletCache
import cash.p.terminal.entities.EvmAddressLabel
import cash.p.terminal.entities.EvmMethodLabel
import cash.p.terminal.entities.EvmSyncSourceRecord
import cash.p.terminal.entities.LogEntry
import cash.p.terminal.entities.RestoreSettingRecord
import cash.p.terminal.entities.StatRecord
import cash.p.terminal.entities.SyncerState
import cash.p.terminal.entities.TokenAutoEnabledBlockchain
import cash.p.terminal.entities.nft.NftAssetBriefMetadataRecord
import cash.p.terminal.entities.nft.NftAssetRecord
import cash.p.terminal.entities.nft.NftCollectionRecord
import cash.p.terminal.entities.nft.NftMetadataSyncRecord
import cash.p.terminal.modules.chart.ChartIndicatorSetting
import cash.p.terminal.modules.chart.ChartIndicatorSettingsDao
import cash.p.terminal.modules.pin.core.Pin
import cash.p.terminal.modules.pin.core.PinDao
import cash.p.terminal.modules.profeatures.storage.ProFeaturesDao
import cash.p.terminal.modules.profeatures.storage.ProFeaturesSessionKey
import cash.p.terminal.modules.walletconnect.storage.WCSessionDao
import cash.p.terminal.modules.walletconnect.storage.WalletConnectV2Session
import cash.p.terminal.wallet.entities.AccountRecord
import cash.p.terminal.wallet.entities.EnabledWallet

@Database(
    version = 62, exportSchema = false, entities = [
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
        CexAssetRaw::class,
        ChartIndicatorSetting::class,
        Pin::class,
        StatRecord::class,
        ChangeNowTransaction::class
    ]
)

@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chartIndicatorSettingsDao(): ChartIndicatorSettingsDao
    abstract fun cexAssetsDao(): CexAssetsDao
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
    abstract fun changeNowTransactionsDao(): ChangeNowTransactionsDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "db_p_cash")
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
                            Migration_61_62
                    )
                    .build()
        }

    }
}
