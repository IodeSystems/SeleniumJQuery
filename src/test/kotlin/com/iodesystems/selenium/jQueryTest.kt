package com.iodesystems.selenium

import io.github.bonigarcia.wdm.WebDriverManager
import org.apache.commons.io.output.NullOutputStream
import org.junit.Test
import org.openqa.selenium.Keys
import org.openqa.selenium.OutputType
import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level


class jQueryTest {

  fun cause(ex: Throwable): Throwable {
    var e = when (ex) {
      is InvocationTargetException ->
        ex.targetException

      else -> ex
    }
    while (e.cause != null && e.cause != e) {
      e = e.cause!!
      when (e) {
        is InvocationTargetException ->
          e = e.targetException
      }
    }
    return e
  }

  fun subject(fn: (jq: jQuery) -> Unit) {
    WebDriverManager.chromedriver().setup()
    val chromeDriverService: ChromeDriverService = ChromeDriverService.Builder()
      .withSilent(true)
      .build()
    chromeDriverService.sendOutputTo(NullOutputStream.INSTANCE)
    val options = ChromeOptions()
    options.addArguments("--auto-open-devtools-for-tabs")
    options.addArguments("--remote-allow-origins=*")
    // If you want to see the browser, comment this out
//    options.addArguments("--headless=new").addArguments("window-size=1920,1080")
    val logPrefs = LoggingPreferences()
    logPrefs.enable(LogType.BROWSER, Level.ALL)
    options.setCapability("goog:loggingPrefs", logPrefs)
    options.setPageLoadStrategy(PageLoadStrategy.EAGER)
    val driver = ChromeDriver(chromeDriverService, options)
    driver.manage().timeouts().apply {
      scriptTimeout(Duration.ofSeconds(5))
    }
    val jq = jQuery(
      driver,
      logQueriesToBrowser = true,
      logQueriesToStdout = true
    )
    try {
      fn(jq)
    } catch (ex: Throwable) {
      val e = cause(ex)
      val sb = StringWriter()
      e.printStackTrace(PrintWriter(sb))
      val banned = listOf(
        "java.base/java.lang.reflect",
        "org.openqa.selenium.remote.codec",
        "org.openqa.selenium.remote.RemoteWebElement.execute",
        "org.openqa.selenium.remote.service.DriverCommandExecutor",
        "org.springframework.aop",
        "org.openqa.selenium.remote.RemoteWebDriver.execute",
        "org.openqa.selenium.remote.HttpCommandExecutor.execute",
        "org.openqa.selenium.support.ui.FluentWait",
        "com.iodesystems.selenium.jQuery\$waitFor",
        "com.iodesystems.selenium.jQuery.waitFor",
        "org.springframework.test",
        "\$El",
        "\$IEl",
        "\$Factory"
      )
      val stack = sb.toString().lines().filter { line ->
        banned.find { ban -> line.contains(ban) } == null
      }.joinToString("\n")
      val onUrl = "On url: " + driver.currentUrl
      val tmp = File.createTempFile(
        "screenshot-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), ".png"
      )
      Files.move(
        driver.getScreenshotAs(OutputType.FILE).absoluteFile.toPath(),
        tmp.toPath(),
        StandardCopyOption.REPLACE_EXISTING
      )
      val screenshot = "See a screenshot at file://" + tmp.absoluteFile
      val message = e.message + """
          $onUrl
          $screenshot
          With a simplified stack:
        """.trimIndent() + stack
      jq.el().sendKeys(Keys.chord(Keys.COMMAND, Keys.CONTROL, "j"))
      println(message)
      println("Browser Logs:")
      val logs = jq.driver.manage().logs()
      println(logs.availableLogTypes.map { logger ->
        logs.get(logger)
      }
        .flatten()
        .filter { line -> line.message.contains("The user aborted a request") }
        .joinToString("\n"))
      assert(false)
    } finally {
      driver.quit()
    }
  }

  @Test
  fun testGoogleSearch() {
    subject { jq ->
      jq.go("http://google.com") {
        icontains("GOOGLE").exists()
        contains("GOOGLE").gone()
        find("textarea[name='q']").sendKeys("hello world", 30)
        find("input[value='Google Search']").first().click()
      }
    }
  }
}

