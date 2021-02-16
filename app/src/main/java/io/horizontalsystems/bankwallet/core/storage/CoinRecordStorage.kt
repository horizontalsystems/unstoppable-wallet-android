package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.ICoinRecordStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinRecord
import io.horizontalsystems.bankwallet.entities.CoinType

class CoinRecordStorage(private val appDatabase: AppDatabase) : ICoinRecordStorage {

    private val dao: CoinRecordDao by lazy {
        appDatabase.coinRecordDao()
    }

    override val coins: List<Coin>
        get() {
            val coinRecords = dao.coinRecords()
            return coinRecords.mapNotNull { coin(it) }
        }

    override fun save(coin: Coin): Boolean {
        when (coin.type) {
            is CoinType.Erc20 -> {
                val record = coinRecord(coin, TokenType.Erc20)
                record.erc20Address = coin.type.address
                dao.insert(record)

                return true
            }
            is CoinType.Bep20 -> {
                val record = coinRecord(coin, TokenType.Bep20)
                record.erc20Address = coin.type.address
                dao.insert(record)

                return true
            }
            is CoinType.Binance -> {
                val record = coinRecord(coin, TokenType.Bep2)
                record.bep2Symbol = coin.type.symbol
                dao.insert(record)

                return true
            }

            else -> { }
        }

        return false
    }

    override fun delete(coin: Coin) {
        dao.delete(coin.coinId)
    }

    override fun deleteAll() {
        dao.deleteAll()
    }

    private fun coin(record: CoinRecord): Coin? {
        val tokenType = try {
            TokenType.valueOf(record.tokenType)
        } catch (e: Exception) {
            return null
        }

        when (tokenType) {
            TokenType.Erc20 -> {
                val address = record.erc20Address ?: return null
                return coin(record, CoinType.Erc20(address))
            }
            TokenType.Bep20 -> {
                val address = record.erc20Address ?: return null
                return coin(record, CoinType.Bep20(address))
            }
            TokenType.Bep2 -> {
                val symbol = record.bep2Symbol ?: return null
                return coin(record, CoinType.Binance(symbol))
            }
            else -> return null
        }
    }

    private fun coinRecord(coin: Coin, tokenType: TokenType): CoinRecord {
        return CoinRecord(coin.coinId, coin.title, coin.code, coin.decimal, tokenType.value)
    }

    private fun coin(record: CoinRecord, coinType: CoinType): Coin {
        return Coin(record.coinId, record.title, record.code, record.decimal, coinType)
    }

    enum class TokenType(val value: String) {
        Erc20("Erc20"), Bep20("Bep20"), Bep2("Bep2")
    }
}
