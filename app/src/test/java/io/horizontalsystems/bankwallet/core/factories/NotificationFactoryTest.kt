package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IEmojiHelper
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import junit.framework.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.math.abs

class NotificationFactoryTest : Spek({

    val context by memoized { mock<Context>() }
    val emojiHelper by memoized { mock<IEmojiHelper>() }
    val factory by memoized { NotificationFactory(emojiHelper, context) }

    val mockBodyString = "body"
    val mockTitleString = "title"
    val mockMultiString = "multi"

    beforeEachTest {
        whenever(emojiHelper.body(any())).thenReturn(mockBodyString)
        whenever(emojiHelper.title(any())).thenReturn(mockTitleString)
        whenever(emojiHelper.multiAlerts).thenReturn(mockMultiString)

        whenever(context.getString(R.string.Notification_Up)).thenReturn("Up")
        whenever(context.getString(R.string.Notification_Down)).thenReturn("Down")
        whenever(context.getString(R.string.Notification_Notifications, emojiHelper.multiAlerts)).thenReturn("Notifications $mockMultiString")
    }

    val firstCoin = Coin("FRST", "First", "FRST", 0, CoinType.Bitcoin)
    val secondCoin = Coin("SCND", "Second", "SCND", 0, CoinType.Bitcoin)
    val thirdCoin = Coin("THRD", "Third", "THRD", 0, CoinType.Bitcoin)
    val fourthCoin = Coin("FRTH", "Fourth", "FRTH", 0, CoinType.Bitcoin)

    describe("#notifications") {
        describe("make notification for 1 alert") {
            it("makes notification for up") {
                val states: List<Int> = listOf(2, 3, 5)
                states.forEach { state ->
                    val priceAlertItem = PriceAlertItem(firstCoin, state)
                    val notifications = factory.notifications(listOf(priceAlertItem))

                    verify(emojiHelper).title(state)
                    verify(emojiHelper).body(state)

                    assertEquals(1, notifications.size)
                    assertEquals("${firstCoin.title} $mockTitleString", notifications[0].title)
                    assertEquals("Up $state% $mockBodyString", notifications[0].body)
                }
            }
            it("makes notification for down") {
                val states: List<Int> = listOf(-2, -3, -5)
                states.forEach { state ->
                    val priceAlertItem = PriceAlertItem(firstCoin, state)
                    val notifications = factory.notifications(listOf(priceAlertItem))

                    verify(emojiHelper).title(state)
                    verify(emojiHelper).body(state)

                    assertEquals(1, notifications.size)
                    assertEquals("${firstCoin.title} $mockTitleString", notifications[0].title)
                    assertEquals("Down ${abs(state)}% $mockBodyString", notifications[0].body)
                }
            }
        }
        describe("make notification for 2 alerts") {
            it("makes 2 notifications") {
                val state = 2
                val firstPriceAlert = PriceAlertItem(firstCoin, state)
                val secondPriceAlert = PriceAlertItem(secondCoin, state)
                val notifications = factory.notifications(listOf(firstPriceAlert, secondPriceAlert))

                verify(emojiHelper, times(2)).title(state)
                verify(emojiHelper, times(2)).body(state)

                assertEquals(2, notifications.size)
            }
        }
        describe("make notification for 3 alerts") {
            it("makes multi notification") {
                val firstState = 2
                val secondState = -5
                val thirdState = 3
                val fourthState = -2
                val alerts = listOf(PriceAlertItem(secondCoin, secondState),
                        PriceAlertItem(firstCoin, firstState),
                        PriceAlertItem(fourthCoin, fourthState),
                        PriceAlertItem(thirdCoin, thirdState)
                )
                val notifications = factory.notifications(alerts)

                assertEquals(1, notifications.size)
                assertEquals("Notifications $mockMultiString", notifications[0].title)

                var body = "${thirdCoin.code} Up $thirdState%, "
                body += "${firstCoin.code} Up $firstState%, "
                body += "${fourthCoin.code} Down ${abs(fourthState)}%, "
                body += "${secondCoin.code} Down ${abs(secondState)}%"
                assertEquals(body, notifications[0].body)
            }
        }

    }
})
