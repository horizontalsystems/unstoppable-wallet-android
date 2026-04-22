package cash.p.terminal.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import cash.p.terminal.core.storage.migrations.Migration_62_63
import cash.p.terminal.core.storage.migrations.Migration_63_64
import cash.p.terminal.core.storage.migrations.Migration_64_65
import cash.p.terminal.core.storage.migrations.Migration_65_66
import cash.p.terminal.core.storage.migrations.Migration_66_67
import cash.p.terminal.core.storage.migrations.Migration_67_68
import cash.p.terminal.core.storage.migrations.Migration_68_69
import cash.p.terminal.core.storage.migrations.Migration_69_70
import cash.p.terminal.core.storage.migrations.Migration_70_71
import cash.p.terminal.core.storage.migrations.Migration_71_72
import cash.p.terminal.core.storage.migrations.Migration_72_73
import cash.p.terminal.core.storage.migrations.Migration_73_74
import cash.p.terminal.core.storage.migrations.Migration_74_75
import cash.p.terminal.core.storage.migrations.Migration_75_76
import cash.p.terminal.core.storage.migrations.Migration_76_77
import cash.p.terminal.core.storage.migrations.Migration_77_78
import cash.p.terminal.core.storage.migrations.Migration_78_79
import cash.p.terminal.core.storage.migrations.Migration_79_80
import cash.p.terminal.core.storage.migrations.Migration_80_81
import cash.p.terminal.core.storage.migrations.Migration_81_82
import cash.p.terminal.core.storage.migrations.Migration_82_83
import cash.p.terminal.core.storage.migrations.Migration_83_84
import cash.p.terminal.core.storage.migrations.Migration_84_85
import cash.p.terminal.core.storage.migrations.Migration_85_86
import cash.p.terminal.core.storage.migrations.Migration_86_87
import cash.p.terminal.core.storage.migrations.Migration_87_88
import cash.p.terminal.core.storage.migrations.Migration_88_89
import cash.p.terminal.core.storage.migrations.Migration_89_90
import cash.p.terminal.core.storage.migrations.Migration_90_91
import cash.p.terminal.core.storage.migrations.Migration_91_92
import cash.p.terminal.core.storage.migrations.Migration_92_93
import cash.p.terminal.core.storage.migrations.Migration_93_94
import cash.p.terminal.core.storage.migrations.Migration_94_95
import cash.p.terminal.core.storage.migrations.Migration_95_96
import cash.p.terminal.core.storage.migrations.Migration_96_97
import cash.p.terminal.core.storage.migrations.Migration_97_98
import cash.p.terminal.core.storage.migrations.Migration_98_99
import cash.p.terminal.core.storage.migrations.Migration_99_100
import cash.p.terminal.core.storage.migrations.Migration_100_101
import cash.p.terminal.core.storage.typeconverter.DatabaseConverters
import cash.p.terminal.entities.ActiveAccount
import cash.p.terminal.entities.BlockchainSettingRecord
import cash.p.terminal.entities.EnabledWalletCache
import cash.p.terminal.entities.EvmAddressLabel
import cash.p.terminal.entities.PoisonAddress
import cash.p.terminal.entities.EvmMethodLabel
import cash.p.terminal.entities.EvmSyncSourceRecord
import cash.p.terminal.entities.MoneroFileRecord
import cash.p.terminal.entities.PendingMultiSwap
import cash.p.terminal.entities.PendingTransactionEntity
import cash.p.terminal.entities.RecentAddress
import cash.p.terminal.entities.RestoreSettingRecord
import cash.p.terminal.entities.SpamAddress
import cash.p.terminal.entities.SpamScanState
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.entities.SyncerState
import cash.p.terminal.entities.TokenAutoEnabledBlockchain
import cash.p.terminal.entities.UserDeletedWallet
import cash.p.terminal.entities.ZcashSingleUseAddress
import cash.p.terminal.entities.nft.NftAssetBriefMetadataRecord
import cash.p.terminal.entities.nft.NftAssetRecord
import cash.p.terminal.entities.nft.NftCollectionRecord
import cash.p.terminal.entities.nft.NftMetadataSyncRecord
import cash.p.terminal.modules.chart.ChartIndicatorSetting
import cash.p.terminal.modules.chart.ChartIndicatorSettingsDao
import cash.p.terminal.modules.pin.core.Pin
import cash.p.terminal.modules.pin.core.PinDao
import cash.p.terminal.modules.walletconnect.storage.WCSessionDao
import cash.p.terminal.modules.walletconnect.storage.WalletConnectV2Session
import cash.p.terminal.wallet.entities.AccountRecord
import cash.p.terminal.wallet.entities.EnabledWallet
import cash.p.terminal.wallet.entities.HardwarePublicKey
import io.horizontalsystems.core.storage.LogEntry
import io.horizontalsystems.core.storage.LogsDao

@Database(
    version = 101,
    exportSchema = false,
    entities = [
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
        EvmAddressLabel::class,
        EvmMethodLabel::class,
        SyncerState::class,
        TokenAutoEnabledBlockchain::class,
        ChartIndicatorSetting::class,
        Pin::class,
        SwapProviderTransaction::class,
        HardwarePublicKey::class,
        RecentAddress::class,
        SpamAddress::class,
        SpamScanState::class,
        MoneroFileRecord::class,
        PendingMultiSwap::class,
        PendingTransactionEntity::class,
        ZcashSingleUseAddress::class,
        UserDeletedWallet::class,
        PoisonAddress::class
    ]
)
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
    abstract fun evmAddressLabelDao(): EvmAddressLabelDao
    abstract fun evmMethodLabelDao(): EvmMethodLabelDao
    abstract fun syncerStateDao(): SyncerStateDao
    abstract fun tokenAutoEnabledBlockchainDao(): TokenAutoEnabledBlockchainDao
    abstract fun spamAddressDao(): SpamAddressDao
    abstract fun pinDao(): PinDao
    abstract fun swapProviderTransactionsDao(): SwapProviderTransactionsDao
    abstract fun hardwarePublicKeyDao(): HardwarePublicKeyDao
    abstract fun recentAddressDao(): RecentAddressDao
    abstract fun moneroFileDao(): MoneroFileDao
    abstract fun pendingMultiSwapDao(): PendingMultiSwapDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
    abstract fun zcashSingleUseAddressDao(): ZcashSingleUseAddressDao
    abstract fun userDeletedWalletDao(): UserDeletedWalletDao
    abstract fun poisonAddressDao(): PoisonAddressDao

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
                    Migration_76_77,
                    Migration_77_78,
                    Migration_78_79,
                    Migration_79_80,
                    Migration_80_81,
                    Migration_81_82,
                    Migration_82_83,
                    Migration_83_84,
                    Migration_84_85,
                    Migration_85_86,
                    Migration_86_87,
                    Migration_87_88,
                    Migration_88_89,
                    Migration_89_90,
                    Migration_90_91,
                    Migration_91_92,
                    Migration_92_93,
                    Migration_93_94,
                    Migration_94_95,
                    Migration_95_96,
                    Migration_96_97,
                    Migration_97_98,
                    Migration_98_99,
                    Migration_99_100,
                    Migration_100_101
                )
                .build()
        }

    }
}
