package io.horizontalsystems.walletkit.modules.privatesend

import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.walletkit.modules.multiswap.providers.memoDelivery
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

/**
 * Builds the deposit transfer for a committed private send order: exactly
 * [PrivateSendOrder.depositAmount] to [PrivateSendOrder.depositAddress], carrying the order's
 * attachment only when the chain actually delivers it to the provider.
 */
object PrivateSendDepositBuilder {

    fun build(order: PrivateSendOrder, btcParams: PrivateSendBtcParams? = null): SendTransactionData {
        val token = order.request.token
        val memo = deliverableMemo(order.attachment, token.blockchainType)

        // EVM needs the kit for ERC20 transfer calldata, so its plugin builds the data.
        // (No EVM path carries a memo, and the gate above already rejected any attachment.)
        ChainRegistry[token.blockchainType]?.depositTransferData(token, order.depositAmount, order.depositAddress)?.let {
            return it
        }

        val address = order.depositAddress
        val amount = order.depositAmount

        return when (token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
                -> SendTransactionData.Btc(
                address = address,
                memo = memo,
                amount = amount,
                // The send screen's settings, so a private send honours the user's coin
                // control, fee rate, sorting and RBF choice. No timelock: the deposit must
                // be spendable by the provider immediately.
                recommendedGasRate = btcParams?.feeRate,
                minimumSendAmount = null,
                changeToFirstInput = false,
                utxoFilters = UtxoFilters(),
                unspentOutputs = btcParams?.unspentOutputs,
                transactionSorting = btcParams?.transactionSorting,
                rbfEnabled = btcParams?.rbfEnabled ?: false,
            )

            BlockchainType.Tron -> {
                // A simple Tron send cannot carry the provider's crediting identifier.
                if (memo != null) throw PrivateSendError.AttachmentUnsupported
                SendTransactionData.Tron.Simple(address, amount)
            }

            BlockchainType.Solana -> {
                if (memo != null) throw PrivateSendError.AttachmentUnsupported
                SendTransactionData.Solana.Simple(address, amount)
            }

            BlockchainType.Stellar -> SendTransactionData.Stellar.Regular(
                address = address,
                memo = memo.orEmpty(),
                amount = amount,
            )

            BlockchainType.Ton -> SendTransactionData.Ton.Regular(
                address = address,
                amount = amount,
                memo = memo,
            )

            // No Zcash/Monero/Zano branches: those chains' own transactions already hide the
            // sender, so PrivateSendManager excludes them outright — see its privateChains.

            BlockchainType.Thorchain,
            BlockchainType.Mayachain,
                -> SendTransactionData.Thorchain.Send(
                address = address,
                amount = amount,
                memo = memo.orEmpty(),
            )

            else -> throw PrivateSendError.CommitFailed()
        }
    }

    /**
     * Only an attachment carried by a plain transfer this app builds — and only on a chain
     * where that memo actually reaches the deposit-address owner — may proceed. Anything else
     * must fail the send rather than be dropped: the provider matches the incoming deposit to
     * the order by this identifier, and a deposit it cannot match is typically unrecoverable.
     *
     * Both `text` and `destination_tag` ride the memo field, exactly as the swap deposit path
     * treats them (see Execution.resolvedMemo): every chain built here puts a numeric tag
     * (e.g. a Stellar memo-id) in the same memo slot, and the dedicated XRP separate-field
     * tag is not a chain this builder supports. An unknown attachment kind still refuses.
     *
     * Also called by PrivateSendManager.commit right after the order is committed, so an
     * undeliverable attachment surfaces once as an authored commit error instead of failing
     * later on every re-entry into the deposit build.
     */
    fun deliverableMemo(
        attachment: UnstoppableAPI.Response.Attachment?,
        blockchainType: BlockchainType,
    ): String? {
        attachment ?: return null

        if (attachment.type != "text" && attachment.type != "destination_tag") {
            throw PrivateSendError.AttachmentUnsupported
        }

        if (!blockchainType.memoDelivery.deliversAttachment) {
            throw PrivateSendError.AttachmentUnsupported
        }

        return attachment.value
    }
}
