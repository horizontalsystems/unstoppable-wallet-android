package cash.p.terminal.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cash.p.terminal.core.storage.migrations.*
import cash.p.terminal.entities.*
import cash.p.terminal.entities.nft.NftAssetBriefMetadataRecord
import cash.p.terminal.entities.nft.NftAssetRecord
import cash.p.terminal.entities.nft.NftCollectionRecord
import cash.p.terminal.entities.nft.NftMetadataSyncRecord
import cash.p.terminal.modules.profeatures.storage.ProFeaturesDao
import cash.p.terminal.modules.profeatures.storage.ProFeaturesSessionKey
import cash.p.terminal.modules.walletconnect.entity.WalletConnectSession
import cash.p.terminal.modules.walletconnect.entity.WalletConnectV2Session
import cash.p.terminal.modules.walletconnect.storage.WC1SessionDao
import cash.p.terminal.modules.walletconnect.storage.WC2SessionDao

@Database(version = 50, exportSchema = false, entities = [
    EnabledWallet::class,
    EnabledWalletCache::class,
    AccountRecord::class,
    BlockchainSettingRecord::class,
    EvmSyncSourceRecord::class,
    LogEntry::class,
    FavoriteCoin::class,
    WalletConnectSession::class,
    WalletConnectV2Session::class,
    RestoreSettingRecord::class,
    ActiveAccount::class,
    EvmAccountState::class,
    NftCollectionRecord::class,
    NftAssetRecord::class,
    NftMetadataSyncRecord::class,
    NftAssetBriefMetadataRecord::class,
    ProFeaturesSessionKey::class,
    EvmAddressLabel::class,
    EvmMethodLabel::class,
    SyncerState::class
])

@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walletsDao(): EnabledWalletsDao
    abstract fun enabledWalletsCacheDao(): EnabledWalletsCacheDao
    abstract fun accountsDao(): AccountsDao
    abstract fun blockchainSettingDao(): BlockchainSettingDao
    abstract fun evmSyncSourceDao(): EvmSyncSourceDao
    abstract fun restoreSettingDao(): RestoreSettingDao
    abstract fun logsDao(): LogsDao
    abstract fun marketFavoritesDao(): MarketFavoritesDao
    abstract fun wc1SessionDao(): WC1SessionDao
    abstract fun wc2SessionDao(): WC2SessionDao
    abstract fun evmAccountStateDao(): EvmAccountStateDao
    abstract fun nftDao(): NftDao
    abstract fun proFeaturesDao(): ProFeaturesDao
    abstract fun evmAddressLabelDao(): EvmAddressLabelDao
    abstract fun evmMethodLabelDao(): EvmMethodLabelDao
    abstract fun syncerStateDao(): SyncerStateDao

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
                    .fallbackToDestructiveMigration()
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
                    )
                    .build()
        }

    }
}
