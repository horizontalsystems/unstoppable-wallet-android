package io.horizontalsystems.marketkit.storage

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import io.horizontalsystems.marketkit.models.*

@Dao
interface CoinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(coin: Coin)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(blockchainEntity: BlockchainEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tokenEntity: TokenEntity)

    @Query("SELECT * FROM Coin WHERE uid = :uid LIMIT 1")
    fun getCoin(uid: String): Coin?

    @Query("SELECT * FROM Coin WHERE uid IN (:uids)")
    fun getCoins(uids: List<String>): List<Coin>

    @Query("SELECT * FROM Coin")
    fun getAllCoins(): List<Coin>

    @RawQuery
    fun getFullCoins(query: SupportSQLiteQuery): List<FullCoinWrapper>

    @Query("SELECT * FROM Coin WHERE uid = :uid LIMIT 1")
    fun getFullCoin(uid: String): FullCoinWrapper?

    @Query("SELECT * FROM Coin WHERE uid IN (:uids)")
    fun getFullCoins(uids: List<String>): List<FullCoinWrapper>

    @RawQuery
    fun getToken(query: SupportSQLiteQuery): TokenWrapper?

    @RawQuery
    fun getTokens(filter: SimpleSQLiteQuery): List<TokenWrapper>

    @Query("SELECT * FROM BlockchainEntity WHERE uid = :uid LIMIT 1")
    fun getBlockchain(uid: String): BlockchainEntity?

    @Query("SELECT * FROM BlockchainEntity WHERE uid IN (:uids)")
    fun getBlockchains(uids: List<String>): List<BlockchainEntity>

    @Query("SELECT * FROM BlockchainEntity")
    fun getAllBlockchains(): List<BlockchainEntity>

    @Query("DELETE FROM Coin")
    fun deleteAllCoins()

    @Query("DELETE FROM BlockchainEntity")
    fun deleteAllBlockchains()

    @Query("DELETE FROM TokenEntity")
    fun deleteAllTokens()

    data class FullCoinWrapper(
        @Embedded
        val coin: Coin,

        @Relation(
            entity = TokenEntity::class,
            parentColumn = "uid",
            entityColumn = "coinUid"
        )
        val tokens: List<TokenEntityWrapper>
    ) {

        val fullCoin: FullCoin
        get() = FullCoin(
            coin,
            tokens.map { it.token(coin) }
        )

    }

    data class TokenEntityWrapper(
        @Embedded
        val tokenEntity: TokenEntity,

        @Relation(
            parentColumn = "blockchainUid",
            entityColumn = "uid"
        )
        val blockchainEntity: BlockchainEntity
    ) {

        fun token(coin: Coin): Token {
            val tokenType = if (tokenEntity.decimals != null) {
                TokenType.fromType(
                    tokenEntity.type,
                    tokenEntity.reference
                )
            } else {
                TokenType.Unsupported(
                    tokenEntity.type,
                    tokenEntity.reference
                )
            }

            return Token(
                coin,
                blockchainEntity.blockchain,
                tokenType,
                tokenEntity.decimals ?: 0
            )
        }

    }

    data class TokenWrapper(
        @Embedded
        val tokenEntity: TokenEntity,

        @Relation(
            parentColumn = "coinUid",
            entityColumn = "uid"
        )
        val coin: Coin,

        @Relation(
            parentColumn = "blockchainUid",
            entityColumn = "uid"
        )
        val blockchainEntity: BlockchainEntity,
    ) {

        val token: Token
            get() = TokenEntityWrapper(tokenEntity, blockchainEntity).token(coin)

    }

}
