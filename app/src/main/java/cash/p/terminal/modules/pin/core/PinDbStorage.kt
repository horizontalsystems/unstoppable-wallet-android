package cash.p.terminal.modules.pin.core

import android.util.Log
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.core.storage.SecretString

class PinDbStorage(private val pinDao: PinDao) {

    fun isLastLevelPinSet(): Boolean {
        val lastLevelPin = pinDao.getLastLevelPin()
        return lastLevelPin?.passcode != null
    }

    fun store(passcode: String, level: Int) {
        val passcodeAlreadyInUse = pinDao.getAll().any { it.passcode?.value == passcode }

        if (passcodeAlreadyInUse) {
            throw IllegalStateException("passcodeAlreadyInUse")
        } else {
            val pin = Pin(level, SecretString(passcode))
            pinDao.insert(pin)
        }
    }

    fun clearPasscode(level: Int) {
        val pin = Pin(level, null)
        pinDao.insert(pin)
    }

    fun getLevel(passcode: String): Int? {
        return pinDao.getAll().find {
            Log.e("AAA", "$it")
            it.passcode?.value == passcode
        }?.level
    }

    fun getPinLevelLast(): Int {
        return pinDao.getLastLevelPin()?.level ?: 0
    }

    fun deleteAllFromLevel(level: Int) {
        pinDao.deleteAllFromLevel(level)
    }

    fun isPinSetForLevel(level: Int): Boolean {
        return pinDao.get(level)?.passcode != null
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

    @Query("SELECT * FROM Pin ORDER BY level DESC LIMIT 1")
    fun getLastLevelPin(): Pin?

    @Query("DELETE FROM Pin WHERE level >= :level")
    fun deleteAllFromLevel(level: Int)
}

@Entity(primaryKeys = ["level"])
data class Pin(val level: Int, val passcode: SecretString?)
