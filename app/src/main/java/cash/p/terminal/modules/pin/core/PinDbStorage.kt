package cash.p.terminal.modules.pin.core

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.wallet.entities.SecretString

class PinDbStorage(private val pinDao: PinDao) {

    fun isLastLevelPinSet(): Boolean {
        val lastLevelPin = pinDao.getLastLevelPin()
        return lastLevelPin?.passcode != null
    }

    fun store(passcode: String, level: Int) {
        val pin = Pin(level, SecretString(passcode))
        pinDao.insert(pin)
    }

    fun clearPasscode(level: Int) {
        val pin = Pin(level, null)
        pinDao.insert(pin)
    }

    fun getLevel(passcode: String): Int? {
        return pinDao.getAll().find {
            it.passcode?.value == passcode
        }?.level
    }

    fun getPinLevelLast(): Int {
        return pinDao.getLastLevelPin()?.level ?: 0
    }

    fun deleteAllFromLevel(level: Int) {
        pinDao.deleteAllFromLevel(level)
    }

    fun deleteForLevel(level: Int) {
        pinDao.deleteForLevel(level)
    }

    fun deleteUserLevelsFromLevel(level: Int) {
        pinDao.deleteUserLevelsFromLevel(level)
    }

    fun deleteLogLoggingPinsFromLevel(userLevel: Int) {
        pinDao.deleteLogLoggingPinsFromLevel(PinLevels.logLoggingLevelFor(userLevel))
    }

    fun isPinSetForLevel(level: Int): Boolean {
        return pinDao.get(level)?.passcode != null
    }

    fun getNextHiddenWalletLevel(): Int {
        val minLevel = pinDao.getMinLevel() ?: 0
        return if (minLevel < 0) minLevel - 1 else -1
    }

    fun getAllLevels(): List<Int> {
        return pinDao.getAll()
            .filter { it.passcode != null && PinLevels.isUserLevel(it.level) }
            .map { it.level }
    }
}

@Dao
interface PinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(pin: Pin)

    @Query("SELECT * FROM Pin WHERE level = :level")
    fun get(level: Int): Pin?

    @Query("SELECT * FROM Pin")
    fun getAll() : List<Pin>

    /** Get the last user level PIN, excluding special PINs at level >= SECURE_RESET */
    @Query("SELECT * FROM Pin WHERE level < ${PinLevels.SECURE_RESET} ORDER BY level DESC LIMIT 1")
    fun getLastLevelPin(): Pin?

    @Query("DELETE FROM Pin WHERE level >= :level")
    fun deleteAllFromLevel(level: Int)

    @Query("DELETE FROM Pin WHERE level = :level")
    fun deleteForLevel(level: Int)

    /** Delete user levels in range [level, SECURE_RESET) - excludes reserved levels */
    @Query("DELETE FROM Pin WHERE level >= :level AND level < ${PinLevels.SECURE_RESET}")
    fun deleteUserLevelsFromLevel(level: Int)

    /** Delete log logging PINs in [logLoggingLevel, DELETE_CONTACTS) */
    @Query("DELETE FROM Pin WHERE level >= :logLoggingLevel AND level < ${PinLevels.DELETE_CONTACTS}")
    fun deleteLogLoggingPinsFromLevel(logLoggingLevel: Int)

    @Query("SELECT MIN(level) FROM Pin")
    fun getMinLevel(): Int?
}

object PinLevels {
    const val SECURE_RESET = 10000
    const val LOG_LOGGING_BASE = 10001
    const val DELETE_CONTACTS = 20001

    fun isUserLevel(pinLevel: Int): Boolean {
        return pinLevel < SECURE_RESET
    }

    fun logLoggingLevelFor(userLevel: Int): Int {
        require(userLevel >= 0) { "Log logging PIN not supported for hidden wallets" }
        return LOG_LOGGING_BASE + userLevel
    }

    fun isLogLoggingLevel(pinLevel: Int): Boolean {
        return pinLevel in LOG_LOGGING_BASE until DELETE_CONTACTS
    }

    fun isDeleteContactsLevel(pinLevel: Int): Boolean {
        return pinLevel == DELETE_CONTACTS
    }

    fun resolvedUserLevelAfterUnlock(pinLevel: Int?): Int? {
        return when {
            pinLevel == null -> null
            pinLevel == SECURE_RESET -> 0
            isUserLevel(pinLevel) -> pinLevel
            else -> null
        }
    }
}

@Entity(primaryKeys = ["level"])
data class Pin(val level: Int, val passcode: SecretString?)
