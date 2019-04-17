package io.horizontalsystems.bankwallet

import androidx.lifecycle.MutableLiveData
import java.util.concurrent.atomic.AtomicBoolean

class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val mPending = AtomicBoolean(false)

    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    override fun postValue(value: T?) {
        mPending.set(true)
        super.postValue(value)
    }

    fun call() {
        value = null
    }

}
