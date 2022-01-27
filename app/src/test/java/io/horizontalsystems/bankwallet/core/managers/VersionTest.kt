package io.horizontalsystems.bankwallet.core.managers

import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test

class VersionTest : TestCase(){

    //0 is equal
    //-1 is less
    //1 is greater

    @Test
    fun testVersionCompare() {
        compare("1.0.0", "0.21.1", 1)
        compare("0.33.0", "1.2.1", -1)
        compare("0.21.0", "0.21.1", 0)
        compare("0.22.0", "1.21.1", -1)
        compare("0.22.0", "0.21.1", 1)
        compare("0.22", "0.21.1", 1)
        compare("0.22", "0.21", 1)
        compare("1", "0.0.1", 1)
    }

    @Test
    fun testVersionForUrl(){
        Assert.assertEquals(Version("0.22.0").versionForUrl, "0.22.0")
        Assert.assertEquals(Version("0.22.3").versionForUrl, "0.22.0")
        Assert.assertEquals(Version("1.22.3").versionForUrl, "1.22.0")
        Assert.assertEquals(Version("0.22").versionForUrl, "0.22")
        Assert.assertEquals(Version("0.22.5.3").versionForUrl, "0.22.0")
    }

    private fun compare(version1: String, version2: String, expected: Int) {
        Assert.assertEquals(expected, Version(version1).compareTo(Version(version2)))
    }
}
