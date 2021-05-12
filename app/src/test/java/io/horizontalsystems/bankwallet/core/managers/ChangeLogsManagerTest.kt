package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.entities.AppVersion
import org.junit.Assert
import org.junit.Test

class ChangeLogsManagerTest {
    private val systemInfoManager = mock<ISystemInfoManager>()
    private val localStorage = mock<ILocalStorage>()
    private val changeLogsManager = ChangeLogsManager(systemInfoManager, localStorage)

    private val appVersions = listOf(
            AppVersion("0.19.0", 103),
            AppVersion("0.19.1", 123),
            AppVersion("0.20.0", 153)
    )

    @Test
    fun testFreshInstall() {
        whenever(systemInfoManager.appVersion).thenReturn("0.21.0")
        whenever(localStorage.changelogShownForAppVersion).thenReturn(null)

        val expected = false
        val actual = changeLogsManager.shouldShowChangeLog()

        Assert.assertEquals(expected, actual)
        verify(localStorage).changelogShownForAppVersion = "0.21.0"
    }

    @Test
    fun testUpdateOfNewerVersion() {
        whenever(systemInfoManager.appVersion).thenReturn("0.21.0")
        whenever(localStorage.changelogShownForAppVersion).thenReturn("0.20.0")

        val expected = true
        val actual = changeLogsManager.shouldShowChangeLog()

        Assert.assertEquals(expected, actual)
        verify(localStorage).changelogShownForAppVersion = "0.21.0"
    }

    @Test
    fun testUpdateOfNewerVersionWithMajorNumber() {
        whenever(systemInfoManager.appVersion).thenReturn("1.0.0")
        whenever(localStorage.changelogShownForAppVersion).thenReturn("0.25.0")

        val expected = true
        val actual = changeLogsManager.shouldShowChangeLog()

        Assert.assertEquals(expected, actual)
        verify(localStorage).changelogShownForAppVersion = "1.0.0"
    }

    @Test
    fun testUpdateOfVersion21() {
        whenever(systemInfoManager.appVersion).thenReturn("0.21.0")
        whenever(localStorage.changelogShownForAppVersion).thenReturn(null)
        whenever(localStorage.appVersions).thenReturn(appVersions)

        val expected = true
        val actual = changeLogsManager.shouldShowChangeLog()

        Assert.assertEquals(expected, actual)
        verify(localStorage).changelogShownForAppVersion = "0.21.0"
    }

    @Test
    fun testRepeatedDisplayOfChangelog() {
        whenever(systemInfoManager.appVersion).thenReturn("0.21.0")
        whenever(localStorage.changelogShownForAppVersion).thenReturn("0.21.0")

        val expected = false
        val actual = changeLogsManager.shouldShowChangeLog()

        Assert.assertEquals(expected, actual)
    }
}
