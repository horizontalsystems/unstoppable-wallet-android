package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.entities.isMaxValue
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData.AdditionalInfo
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.erc20kit.decorations.ApproveEip20Decoration
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.nftkit.decorations.OutgoingEip1155Decoration
import io.horizontalsystems.nftkit.decorations.OutgoingEip721Decoration
import io.horizontalsystems.oneinchkit.decorations.OneInchDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapDecoration
import java.math.BigInteger

class SendEvmTransactionViewItemFactory(
    private val evmLabelManager: EvmLabelManager,
    private val coinServiceFactory: EvmCoinServiceFactory,
    private val contactsRepo: ContactsRepository,
    private val blockchainType: BlockchainType
) {
    fun getItems(
        transactionData: TransactionData?,
        additionalInfo: AdditionalInfo?,
        decoration: TransactionDecoration?
    ): List<SectionViewItem> {
        var sections = decoration?.let {
            getViewItems(it, additionalInfo)
        } ?: listOf()

        if (sections.isEmpty()) {
            if (transactionData != null) {
                sections = getUnknownMethodItems(
                    transactionData,
                    evmLabelManager.methodLabel(transactionData.input)
                )
            }
        }

        return sections
    }

    private fun getViewItems(
        decoration: TransactionDecoration,
        additionalInfo: AdditionalInfo?
    ): List<SectionViewItem>? =
        when (decoration) {
            is OutgoingDecoration -> getSendBaseCoinItems(
                decoration.to,
                decoration.value
            )

            is OutgoingEip20Decoration -> getEip20TransferViewItems(
                decoration.to,
                decoration.value,
                decoration.contractAddress
            )

            is ApproveEip20Decoration -> getEip20ApproveViewItems(
                decoration.spender,
                decoration.value,
                decoration.contractAddress
            )

            is SwapDecoration -> getUniswapViewItems(
                decoration.amountIn,
                decoration.amountOut,
                decoration.tokenIn,
                decoration.tokenOut,
                decoration.recipient
            )

            is OneInchSwapDecoration -> getOneInchSwapViewItems(
                decoration.tokenIn,
                decoration.tokenOut,
                decoration.amountIn,
                decoration.amountOut
            )

            is OneInchUnoswapDecoration -> getOneInchSwapViewItems(
                decoration.tokenIn,
                decoration.tokenOut,
                decoration.amountIn,
                decoration.amountOut
            )

            is OneInchDecoration -> null

            is OutgoingEip721Decoration -> getNftTransferItems(
                decoration.to,
                BigInteger.ONE,
                additionalInfo?.sendInfo,
                decoration.tokenId,
            )

            is OutgoingEip1155Decoration -> getNftTransferItems(
                decoration.to,
                decoration.value,
                additionalInfo?.sendInfo,
                decoration.tokenId,
            )

            else -> null
        }

    private fun getNftTransferItems(
        recipient: Address,
        value: BigInteger,
        sendInfo: SendEvmData.SendInfo?,
        tokenId: BigInteger
    ): List<SectionViewItem> {

        val sections = mutableListOf<SectionViewItem>()
        val addressValue = recipient.eip55

        val viewItems = buildList {
            add(
                ViewItem.Subhead(
                    Translator.getString(R.string.Send_Confirmation_YouSend),
                    sendInfo?.nftShortMeta?.nftName ?: tokenId.toString(),
                    R.drawable.ic_arrow_up_right_12
                )
            )

            add(
                getNftAmount(
                    value,
                    sendInfo?.nftShortMeta?.previewImageUrl
                )
            )

            val contact = getContact(addressValue)

            add(
                ViewItem.Address(
                    Translator.getString(R.string.Send_Confirmation_To),
                    addressValue,
                    contact == null,
                    blockchainType,
                    StatSection.AddressTo
                )
            )

            contact?.let {
                add(
                    ViewItem.ContactItem(it)
                )
            }
        }

        sections.add(SectionViewItem(viewItems))

        return sections
    }

    private fun getUniswapViewItems(
        amountIn: SwapDecoration.Amount,
        amountOut: SwapDecoration.Amount,
        tokenIn: SwapDecoration.Token,
        tokenOut: SwapDecoration.Token,
        recipient: Address?
    ): List<SectionViewItem>? {

        val coinServiceIn = getCoinService(tokenIn) ?: return null
        val coinServiceOut = getCoinService(tokenOut) ?: return null

        val sections = mutableListOf<SectionViewItem>()
        val inViewItems = mutableListOf<ViewItem>()
        val outViewItems = mutableListOf<ViewItem>()
        val otherViewItems = mutableListOf<ViewItem>()

        if (recipient != null) {
            val addressValue = recipient.eip55
            val contact = getContact(addressValue)
            otherViewItems.add(
                ViewItem.Address(
                    Translator.getString(R.string.SwapSettings_RecipientAddressTitle),
                    addressValue,
                    contact == null,
                    blockchainType,
                    StatSection.AddressRecipient
                )
            )
            contact?.let {
                otherViewItems.add(
                    ViewItem.ContactItem(it)
                )
            }
        }

        when (amountIn) {
            is SwapDecoration.Amount.Exact -> { // you pay exact
                val amountData = coinServiceIn.amountData(amountIn.value)
                inViewItems.add(getAmount(amountData, ValueType.Outgoing, coinServiceIn.token))
            }

            is SwapDecoration.Amount.Extremum -> { // you pay estimated
                val maxAmountData = coinServiceIn.amountData(amountIn.value)
                inViewItems.add(getMaxAmount(maxAmountData, coinServiceIn.token))
            }
        }

        when (amountOut) {
            is SwapDecoration.Amount.Exact -> { // you get exact
                val amountData = coinServiceOut.amountData(amountOut.value)
                outViewItems.add(
                    getAmount(amountData, ValueType.Incoming, coinServiceOut.token)
                )
            }

            is SwapDecoration.Amount.Extremum -> { // you get estimated
                val guaranteedAmountData = coinServiceOut.amountData(amountOut.value)
                outViewItems.add(getGuaranteedAmount(guaranteedAmountData, coinServiceOut.token))
            }
        }

        inViewItems.add(
            0, ViewItem.Subhead(
                Translator.getString(R.string.Swap_FromAmountTitle),
                coinServiceIn.token.coin.name,
                R.drawable.ic_arrow_up_right_12
            )
        )
        sections.add(SectionViewItem(inViewItems))

        outViewItems.add(
            0, ViewItem.Subhead(
                Translator.getString(R.string.Swap_ToAmountTitle),
                coinServiceOut.token.coin.name,
                R.drawable.ic_arrow_down_left_12
            )
        )
        sections.add(SectionViewItem(outViewItems))

        if (otherViewItems.isNotEmpty()) {
            sections.add(SectionViewItem(otherViewItems))
        }

        return sections
    }

    private fun getOneInchSwapViewItems(
        tokenIn: OneInchDecoration.Token,
        tokenOut: OneInchDecoration.Token?,
        amountIn: BigInteger,
        amountOut: OneInchDecoration.Amount
    ): List<SectionViewItem>? {
        val coinServiceIn = getCoinService(tokenIn) ?: return null
        val coinServiceOut = tokenOut?.let { getCoinService(it) } ?: return null

        val sections = mutableListOf<SectionViewItem>()

        sections.add(
            SectionViewItem(
                listOf(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Swap_FromAmountTitle),
                        coinServiceIn.token.coin.name,
                        R.drawable.ic_arrow_up_right_12
                    ),
                    getAmount(
                        coinServiceIn.amountData(amountIn),
                        ValueType.Outgoing,
                        coinServiceIn.token
                    )
                )
            )
        )

        val outViewItems: MutableList<ViewItem> = mutableListOf(
            ViewItem.Subhead(
                Translator.getString(R.string.Swap_ToAmountTitle),
                coinServiceOut.token.coin.name,
                R.drawable.ic_arrow_down_left_12
            )
        )

        if (amountOut is OneInchDecoration.Amount.Extremum) {
            val guaranteedAmountData = coinServiceOut.amountData(amountOut.value)

            outViewItems.add(getGuaranteedAmount(guaranteedAmountData, coinServiceOut.token))
        }
        sections.add(SectionViewItem(outViewItems))

        return sections
    }

    private fun getEip20TransferViewItems(
        to: Address,
        value: BigInteger,
        contractAddress: Address
    ): List<SectionViewItem>? {
        val coinService = coinServiceFactory.getCoinService(contractAddress) ?: return null

        val viewItems: MutableList<ViewItem> = mutableListOf(
            getAmountWithTitle(
                coinService.amountData(value),
                ValueType.Outgoing,
                coinService.token,
                Translator.getString(R.string.Send_Confirmation_YouSend),
                coinService.token.badge
            )
        )
        val addressValue = to.eip55
        val contact = getContact(addressValue)
        viewItems.add(
            ViewItem.Address(
                Translator.getString(R.string.Send_Confirmation_To),
                addressValue,
                contact == null,
                blockchainType,
                StatSection.AddressTo
            )
        )
        contact?.let {
            viewItems.add(
                ViewItem.ContactItem(it)
            )
        }

        return listOf(SectionViewItem(viewItems))
    }

    private fun getContact(addressValue: String): Contact? {
        return contactsRepo.getContactsFiltered(blockchainType, addressQuery = addressValue).firstOrNull()
    }

    private fun getEip20ApproveViewItems(
        spender: Address,
        value: BigInteger,
        contractAddress: Address
    ): List<SectionViewItem>? {
        val coinService = coinServiceFactory.getCoinService(contractAddress) ?: return null

        val viewItems = buildList {
            if (value.compareTo(BigInteger.ZERO) == 0) {
                add(
                    ViewItem.Subhead(
                        Translator.getString(R.string.Approve_YouRevoke),
                        coinService.token.coin.name,
                    )
                )
                add(ViewItem.TokenItem(coinService.token))
            } else {
                add(
                    getAmountWithTitle(
                        coinService.amountData(value),
                        ValueType.Regular,
                        coinService.token,
                        Translator.getString(R.string.Approve_Allowance),
                        coinService.token.badge
                    )
                )
            }

            val addressValue = spender.eip55
            val contact = getContact(addressValue)
            add(
                ViewItem.Address(
                    Translator.getString(R.string.Approve_Spender),
                    addressValue,
                    contact == null,
                    blockchainType,
                    StatSection.AddressSpender
                )
            )
            contact?.let {
                add(
                    ViewItem.ContactItem(it)
                )
            }
        }

        return listOf(SectionViewItem(viewItems))
    }

    private fun getUnknownMethodItems(
        transactionData: TransactionData,
        methodName: String?,
    ): List<SectionViewItem> {
        val viewItems = buildList {
            methodName?.let {
                add(ViewItem.Value(Translator.getString(R.string.Send_Confirmation_Method), it, ValueType.Regular))
            }
            add(
                getAmount(
                    coinServiceFactory.baseCoinService.amountData(transactionData.value),
                    ValueType.Outgoing,
                    coinServiceFactory.baseCoinService.token
                )
            )
            val toValue = transactionData.to.eip55
            val contact = getContact(toValue)
            add(
                ViewItem.Address(
                    Translator.getString(R.string.Send_Confirmation_To),
                    toValue,
                    contact == null,
                    blockchainType,
                    StatSection.AddressTo
                )
            )
            contact?.let {
                add(
                    ViewItem.ContactItem(it)
                )
            }

            add(ViewItem.Input("Input", transactionData.input.toHexString()))
        }

        return listOf(SectionViewItem(viewItems))
    }

    private fun getSendBaseCoinItems(to: Address, value: BigInteger): List<SectionViewItem> {
        val toValue = to.eip55
        val baseCoinService = coinServiceFactory.baseCoinService

        val viewItems = buildList {
            add(
                getAmountWithTitle(
                    baseCoinService.amountData(value),
                    ValueType.Outgoing,
                    baseCoinService.token,
                    Translator.getString(R.string.Send_Confirmation_YouSend),
                    baseCoinService.token.badge,
                )
            )
            val contact = getContact(toValue)
            add(
                ViewItem.Address(
                    Translator.getString(R.string.Send_Confirmation_To),
                    toValue,
                    contact == null,
                    blockchainType,
                    StatSection.AddressTo
                )
            )
            contact?.let {
                add(
                    ViewItem.ContactItem(it)
                )
            }
        }

        return listOf(
            SectionViewItem(
                viewItems
            )
        )
    }

    private fun getCoinService(token: SwapDecoration.Token) = when (token) {
        SwapDecoration.Token.EvmCoin -> coinServiceFactory.baseCoinService
        is SwapDecoration.Token.Eip20Coin -> coinServiceFactory.getCoinService(token.address)
    }

    private fun getCoinService(token: OneInchDecoration.Token) = when (token) {
        OneInchDecoration.Token.EvmCoin -> coinServiceFactory.baseCoinService
        is OneInchDecoration.Token.Eip20Coin -> coinServiceFactory.getCoinService(token.address)
    }

    private fun getNftAmount(value: BigInteger, previewImageUrl: String?): ViewItem.NftAmount =
        ViewItem.NftAmount(previewImageUrl, "$value NFT", ValueType.Regular)

    private fun getAmount(amountData: SendModule.AmountData, valueType: ValueType, token: Token) =
        ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            amountData.primary.getFormatted(),
            valueType,
            token
        )

    private fun getAmountWithTitle(
        amountData: SendModule.AmountData,
        valueType: ValueType,
        token: Token,
        title: String,
        badge: String?
    ): ViewItem.AmountWithTitle {
        return if (amountData.primary.coinValue.value.isMaxValue(amountData.primary.coinValue.decimal)) {
            ViewItem.AmountWithTitle(
                null,
                "∞ ${token.coin.code}",
                valueType,
                token,
                title,
                badge
            )
        } else {
            ViewItem.AmountWithTitle(
                amountData.secondary?.getFormatted(),
                amountData.primary.getFormatted(),
                valueType,
                token,
                title,
                badge
            )
        }
    }

    private fun getGuaranteedAmount(amountData: SendModule.AmountData, token: Token) =
        ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            "${amountData.primary.getFormatted()} ${Translator.getString(R.string.Swap_AmountMin)}",
            ValueType.Incoming,
            token
        )

    private fun getMaxAmount(amountData: SendModule.AmountData, token: Token) =
        ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            "${amountData.primary.getFormatted()} ${Translator.getString(R.string.Swap_AmountMax)}",
            ValueType.Outgoing,
            token
        )
}

data class SectionViewItem(
    val viewItems: List<ViewItem>
)

sealed class ViewItem {
    class Subhead(val title: String, val value: String, val iconRes: Int? = null) : ViewItem()
    class Value(
        val title: String,
        val value: String,
        val type: ValueType,
    ) : ViewItem()

    class ValueMulti(
        val title: String,
        val primaryValue: String,
        val secondaryValue: String,
        val type: ValueType,
    ) : ViewItem()

    class AmountMulti(
        val amounts: List<AmountValues>,
        val type: ValueType,
        val token: Token
    ) : ViewItem()

    class Amount(
        val fiatAmount: String?,
        val coinAmount: String,
        val type: ValueType,
        val token: Token
    ) : ViewItem()

    class AmountWithTitle(
        val fiatAmount: String?,
        val coinAmount: String,
        val type: ValueType,
        val token: Token,
        val title: String,
        val badge: String?
    ) : ViewItem()

    class NftAmount(
        val iconUrl: String?,
        val amount: String,
        val type: ValueType,
    ) : ViewItem()

    class Address(val title: String, val value: String, val showAdd: Boolean, val blockchainType: BlockchainType, val statSection: StatSection) : ViewItem()
    class Input(val title: String, val value: String) : ViewItem()
    class TokenItem(val token: Token) : ViewItem()
    class ContactItem(val contact: Contact) : ViewItem()
    class Fee(val networkFee: SendModule.AmountData) : ViewItem()
}

data class AmountValues(val coinAmount: String, val fiatAmount: String?)
enum class ValueType {
    Regular, Disabled, Outgoing, Incoming, Warning, Forbidden
}