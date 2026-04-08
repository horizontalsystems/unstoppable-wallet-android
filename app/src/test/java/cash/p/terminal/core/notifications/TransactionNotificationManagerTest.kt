package cash.p.terminal.core.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class TransactionNotificationManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val notificationManagerCompat = mockk<NotificationManagerCompat>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(any()) } returns notificationManagerCompat

        mockkConstructor(NotificationCompat.Builder::class)
        val fakeNotification = mockk<Notification>()
        every { anyConstructed<NotificationCompat.Builder>().setSmallIcon(any<Int>()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().setContentTitle(any()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().setContentText(any<CharSequence>()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().setAutoCancel(any()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().setContentIntent(any()) } answers { self as NotificationCompat.Builder }
        every { anyConstructed<NotificationCompat.Builder>().build() } returns fakeNotification
    }

    @After
    fun tearDown() {
        unmockkStatic(NotificationManagerCompat::class)
        unmockkConstructor(NotificationCompat.Builder::class)
    }

    @Test
    fun showTransactionNotification_noPermission_returnsEarly() {
        val manager = spyk(TransactionNotificationManager(context))
        every { manager.hasNotificationPermission() } returns false

        manager.showTransactionNotification("uid-1", "title", "text")

        verify(exactly = 0) { notificationManagerCompat.notify(any<String>(), any(), any()) }
    }

    @Test
    fun showTransactionNotification_withPermission_callsNotify() {
        val manager = spyk(TransactionNotificationManager(context))
        every { manager.hasNotificationPermission() } returns true
        every { manager.isTransactionChannelEnabled() } returns true

        manager.showTransactionNotification("uid-1", "title", "text")

        verify(exactly = 1) { notificationManagerCompat.notify("uid-1", any(), any()) }
    }

    @Test
    fun showTransactionNotification_securityException_doesNotCrash() {
        val manager = spyk(TransactionNotificationManager(context))
        every { manager.hasNotificationPermission() } returns true
        every { manager.isTransactionChannelEnabled() } returns true
        every {
            notificationManagerCompat.notify(any<String>(), any(), any())
        } throws SecurityException("revoked")

        manager.showTransactionNotification("uid-1", "title", "text")

        verify(exactly = 1) { notificationManagerCompat.notify(any<String>(), any(), any()) }
    }

    @Test
    fun showTransactionNotification_channelDisabled_returnsEarly() {
        val manager = spyk(TransactionNotificationManager(context))
        every { manager.hasNotificationPermission() } returns true
        every { manager.isTransactionChannelEnabled() } returns false

        manager.showTransactionNotification("uid-1", "title", "text")

        verify(exactly = 0) { notificationManagerCompat.notify(any<String>(), any(), any()) }
    }
}
