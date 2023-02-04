package com.iodesystems.selenium

import org.apache.commons.io.output.NullOutputStream
import org.junit.Test
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions


class jQueryTest {

    fun subject(fn: (jq: jQuery) -> Unit) {
        val chromeDriverService: ChromeDriverService = ChromeDriverService.Builder().build()
        chromeDriverService.sendOutputTo(NullOutputStream.NULL_OUTPUT_STREAM)
        val driver = ChromeDriver(chromeDriverService, ChromeOptions())
        try {
            fn(jQuery(driver, logQueriesToBrowser = true, logQueriesToStdout = true))
        } finally {
            driver.quit()
        }
    }

    @Test
    fun testGoogleSearch() {
        subject { jq ->
            jq.driver.get("http://google.com")
            jq.page {
                child("input[name='q']").sendKeys("hello world", 30)
                child("input[value='Google Search']").first().click()
                child("#result-stats").exists()
            }
        }
    }
}

