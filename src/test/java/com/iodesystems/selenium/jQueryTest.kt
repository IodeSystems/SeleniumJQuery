package com.iodesystems.selenium

import io.github.bonigarcia.wdm.WebDriverManager
import org.apache.commons.io.output.NullOutputStream
import org.junit.Test
import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions


class jQueryTest {

    fun subject(fn: (jq: jQuery) -> Unit) {
        WebDriverManager.chromedriver().setup()
        val chromeDriverService: ChromeDriverService = ChromeDriverService.Builder()
            .withSilent(true)
            .build()
        chromeDriverService.sendOutputTo(NullOutputStream.NULL_OUTPUT_STREAM)
        val options = ChromeOptions()
        options.setPageLoadStrategy(PageLoadStrategy.EAGER)
        val driver = ChromeDriver(chromeDriverService, options)
        try {
            fn(jQuery(driver, logQueriesToBrowser = true, logQueriesToStdout = true))
        } finally {
            driver.quit()
        }
    }

    @Test
    fun testGoogleSearch() {
        subject { jq ->
            jq.page("http://google.com") {
                find("input[name='q']").sendKeys("hello world", 30)
                find("input[value='Google Search']").first().click()
                find("#result-stats").visible()
            }
        }
    }
}

