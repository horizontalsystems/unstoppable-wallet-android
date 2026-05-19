package cash.p.terminal.wallet.storage

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MarketDatabaseTest {

    @Test
    fun loadInitialCoins_initialCoinList_returnsExecutedLineCount() {
        val context = mockk<Context>()
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val file = initialCoinsFile()
        val inputStream = file.inputStream()

        every { context.assets.open("initial_coins_list") } returns inputStream

        val marketDatabase = MarketDatabase.Companion
        val count = marketDatabase.loadInitialCoins(db, context)

        assertEquals(file.readLines().size, count)
    }

    @Test
    fun initialCoins_litecoin_containsMwebToken() {
        val file = initialCoinsFile()
        val mwebTokenSql = "INSERT OR REPLACE INTO TokenEntity VALUES('litecoin','litecoin','mweb',8,'');"

        assertTrue(file.readLines().contains(mwebTokenSql))
    }

    private fun initialCoinsFile(): File {
        val moduleRelativeFile = File("src/main/assets/initial_coins_list")
        if (moduleRelativeFile.exists()) {
            return moduleRelativeFile
        }

        return File("core/wallet/src/main/assets/initial_coins_list")
    }

}
