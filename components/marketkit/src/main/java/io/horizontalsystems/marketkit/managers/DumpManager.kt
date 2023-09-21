package io.horizontalsystems.marketkit.managers

import android.database.DatabaseUtils
import io.horizontalsystems.marketkit.storage.MarketDatabase

class DumpManager(private val marketDatabase: MarketDatabase) {

    private val tablesCreation =
        "CREATE TABLE IF NOT EXISTS `BlockchainEntity` (`uid` TEXT NOT NULL, `name` TEXT NOT NULL, `eip3091url` TEXT, PRIMARY KEY(`uid`));\n" +
                "CREATE TABLE IF NOT EXISTS `Coin` (`uid` TEXT NOT NULL, `name` TEXT NOT NULL, `code` TEXT NOT NULL, `marketCapRank` INTEGER, `coinGeckoId` TEXT, PRIMARY KEY(`uid`));\n" +
                "CREATE TABLE IF NOT EXISTS `TokenEntity` (`coinUid` TEXT NOT NULL, `blockchainUid` TEXT NOT NULL, `type` TEXT NOT NULL, `decimals` INTEGER, `reference` TEXT NOT NULL, PRIMARY KEY(`coinUid`, `blockchainUid`, `type`, `reference`), FOREIGN KEY(`coinUid`) REFERENCES `Coin`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`blockchainUid`) REFERENCES `BlockchainEntity`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE )\n"

    fun getInitialDump(): String {
        val insertQueries = StringBuilder()
        insertQueries.append(tablesCreation)
        val blockchains = marketDatabase.blockchainEntityDao().getAll()
        blockchains.forEach { blockchain ->
            val eipUrl = blockchain.eip3091url?.let { "'$it'" } ?: "null"
            val insertQuery =
                "INSERT INTO BlockchainEntity VALUES('${blockchain.uid}',${DatabaseUtils.sqlEscapeString(blockchain.name)},$eipUrl);"
            insertQueries.append(insertQuery).append("\n")
        }
        val tokens = marketDatabase.tokenEntityDao().getAll()
        tokens.forEach { token ->
            val reference = token.reference?.let { "'$it'" } ?: "null"
            val insertQuery =
                "INSERT INTO TokenEntity VALUES('${token.coinUid}','${token.blockchainUid}','${token.type}',${token.decimals},$reference);"
            insertQueries.append(insertQuery).append("\n")
        }
        val coins = marketDatabase.coinDao().getAllCoins()
        coins.forEach { coin ->
            val coinGeckoId = coin.coinGeckoId?.let { "'$it'" } ?: "null"
            val insertQuery =
                "INSERT INTO Coin VALUES('${coin.uid}',${DatabaseUtils.sqlEscapeString(coin.name)},'${coin.code}',${coin.marketCapRank},$coinGeckoId);"
            insertQueries.append(insertQuery).append("\n")
        }
        return insertQueries.toString()
    }
}