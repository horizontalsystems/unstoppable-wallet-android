package io.horizontalsystems.bankwallet.modules.pin.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class PinManager(
    private val pinDbStorage: PinDbStorage
) {
    private val _pinSetFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val pinSetFlow: SharedFlow<Unit> = _pinSetFlow.asSharedFlow()


    val isPinSet: Boolean
        get() = pinDbStorage.isLastLevelPinSet()

    @Throws
    suspend fun store(pin: String, level: Int) {
        pinDbStorage.store(pin, level)
        _pinSetFlow.emit(Unit)
    }

    suspend fun getPinLevel(pin: String): Int? = withContext(Dispatchers.IO) {
        pinDbStorage.getLevel(pin)
    }

    fun getPinLevelSync(pin: String): Int? {
        return pinDbStorage.getLevel(pin)
    }

    fun getPinLevelLast(): Int {
        return pinDbStorage.getPinLevelLast()
    }

    suspend fun disablePin(level: Int) {
        pinDbStorage.clearPasscode(level)
        pinDbStorage.deleteAllFromLevel(level + 1)
        _pinSetFlow.emit(Unit)
    }

    suspend fun disableDuressPin(level: Int) {
        pinDbStorage.deleteAllFromLevel(level)
        _pinSetFlow.emit(Unit)
    }

    fun isPinSetForLevel(level: Int): Boolean {
        return pinDbStorage.isPinSetForLevel(level)
    }

    fun isUnique(pin: String, userLevel: Int): Boolean {
        val pinLevel = pinDbStorage.getLevel(pin) ?: return true
        return pinLevel == userLevel
    }
}