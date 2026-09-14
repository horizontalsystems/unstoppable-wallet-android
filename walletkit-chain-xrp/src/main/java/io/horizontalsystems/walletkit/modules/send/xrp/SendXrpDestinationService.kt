package io.horizontalsystems.walletkit.modules.send.xrp

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.ISendXrpAdapter
import io.horizontalsystems.walletkit.core.ServiceState
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.multiswap.providers.XrpDestinationTag
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal

/**
 * Looks up what the destination account demands: a minimum first deposit when it does not
 * exist yet, and a destination tag when it has the RequireDestTag flag.
 */
class SendXrpDestinationService(
    private val adapter: ISendXrpAdapter,
) : ServiceState<SendXrpDestinationService.State>() {

    // setValidAddress runs on the address collector while tag input arrives from the UI
    private val lock = Any()

    private var minimumAmount: BigDecimal? = null
    private var tagRequired = false
    private var lookupError: Throwable? = null
    private var tag: Long? = null
    private var tagError: Throwable? = null

    override fun createState() = State(
        minimumAmount = minimumAmount,
        tagRequired = tagRequired,
        error = lookupError ?: tagError,
        canBeSend = lookupError == null && tagError == null,
    )

    suspend fun setValidAddress(address: Address?) {
        var newMinimumAmount: BigDecimal? = null
        var newTagRequired = false
        var newLookupError: Throwable? = null
        try {
            newMinimumAmount = address?.let { adapter.getMinimumSendAmount(it.hex) }
            newTagRequired = address?.let { adapter.requiresDestinationTag(it.hex) } ?: false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            newMinimumAmount = null
            newTagRequired = false
            newLookupError = e
        }
        synchronized(lock) {
            minimumAmount = newMinimumAmount
            tagRequired = newTagRequired
            lookupError = newLookupError
            validateTag()
            emitState()
        }
    }

    /** [input] is the raw text of the tag field; blank means no tag. */
    fun setTagInput(input: String) = synchronized(lock) {
        val text = input.trim()
        tag = null
        tagError = null
        if (text.isNotEmpty()) {
            val value = text.toLongOrNull()
            if (value == null || value < 0 || value > MAX_TAG) {
                tagError = Throwable(Translator.getString(R.string.Send_DestinationTag_Invalid))
            } else {
                tag = value
            }
        }
        validateTag()
        emitState()
    }

    /** A tag carried by an X-address replaces whatever was typed. */
    fun setFixedTag(fixed: Long?) = synchronized(lock) {
        if (fixed != null) {
            tag = fixed
            tagError = null
        }
        validateTag()
        emitState()
    }

    val destinationTag: Long? get() = tag

    private fun validateTag() {
        if (tagError == null && tagRequired && tag == null) {
            tagError = Throwable(Translator.getString(R.string.Send_DestinationTag_Required))
        }
    }

    data class State(
        val minimumAmount: BigDecimal?,
        val tagRequired: Boolean,
        val error: Throwable?,
        val canBeSend: Boolean,
    )

    companion object {
        const val MAX_TAG = XrpDestinationTag.MAX
    }
}
