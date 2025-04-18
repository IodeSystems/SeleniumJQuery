package com.iodesystems.selenium

import com.iodesystems.selenium.el.El
import com.iodesystems.selenium.el.IEl
import com.iodesystems.selenium.exceptions.RetryException
import org.openqa.selenium.JavascriptException
import org.openqa.selenium.ScriptTimeoutException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.RemoteWebElement
import org.openqa.selenium.support.ui.FluentWait
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.time.Duration


data class jQuery(
  val driver: RemoteWebDriver,
  val timeout: Duration = Duration.ofSeconds(5),
  val logQueriesToBrowser: Boolean = false,
  val logQueriesToStdout: Boolean = false,
  val onInstallScript: String? = null,
) : Closeable {
  private fun install() {
    val shouldRunOnInstallScript = mapOf(
      "jQuery" to "jquery-3.7.1.slim.min.js",
      "SeleniumJQuery" to "selenium-jquery-helpers.js"
    ).map { entry ->
      if (logQueriesToStdout) {
        println("Installing ${entry.key} from ${entry.value}")
      }
      if (driver.executeScript("return typeof window.${entry.key}") == "undefined") {
        val jQueryStream = javaClass.getResourceAsStream("/${entry.value}")
        val jQueryStreamBuffer = ByteArrayOutputStream()
        jQueryStream?.transferTo(jQueryStreamBuffer)
        val jQueryContent = jQueryStreamBuffer.toString()
        driver.executeScript(jQueryContent)
        true
      } else {
        false
      }
    }.any { it }
    if (shouldRunOnInstallScript && onInstallScript != null) {
      driver.executeScript(onInstallScript)
    }
  }

  fun escape(string: String): String {
    return '"' + string.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + '"'
  }

  fun search(els: List<IEl>): List<List<RemoteWebElement>?> {
    return waitForNonNull("Search query to return stable results") {
      try {
        if (logQueriesToStdout) {
          println(els.joinToString("; ") { it.renderScript() } + ";")
        }
        @Suppress("UNCHECKED_CAST")
        driver.executeAsyncScript(
          "SeleniumJQuery.search(arguments[0],arguments[1],arguments[2],arguments[3],arguments[4])",
          logQueriesToBrowser,
          els.map { it.renderSelector() },
          els.map { it.atLeast() },
          els.map { it.atMost() }
        ) as List<List<RemoteWebElement>?>
      } catch (e: StaleElementReferenceException) {
        throw RetryException("Stale element returned", e)
      } catch (e: ScriptTimeoutException) {
        // Render message and selectors
        val messages = els.joinToString(", ") { el ->
          val message = when (el.atMost()) {
            0 -> "not present"
            null -> when (el.atLeast()) {
              0 -> "present"
              null -> "present"
              else -> "at least ${el.atLeast()} elements"
            }

            else -> when (el.atLeast()) {
              0 -> "at most ${el.atMost()} elements"
              null -> "at most ${el.atMost()} elements"
              else -> "between ${el.atLeast()} and ${el.atMost()} elements"
            }
          }
          el.renderSelector() + " to be $message)"
        }
        throw RetryException("Timeout waiting for $messages", e)
      }
    }
  }

  fun waitForJsTrue(
    script: String,
    message: String = "script to return true",
    retryDelay: Duration? = null,
    timeOut: Duration? = null
  ) {
    waitForNonNull(message, retry = retryDelay, timeOut = timeOut) {
      when (val result = driver.executeScript(script)) {
        is Boolean -> if (!result) {
          throw RetryException("Script returned false")
        } else {
          true
        }

        else -> throw RetryException("Script did not return a boolean")
      }
    }
  }

  fun <T> waitForNonNull(
    failureMessage: String,
    retry: Duration? = null,
    timeOut: Duration? = null,
    t: () -> T
  ): T {
    return FluentWait(driver)
      .withTimeout(timeOut ?: timeout)
      .pollingEvery(retry ?: Duration.ofMillis(10))
      .withMessage(failureMessage)
      .ignoring(RetryException::class.java).until {
        try {
          t()
        } catch (e: JavascriptException) {
          install()
          t()
        }
      }
  }

  fun <T> go(url: String): El {
    driver.get(url)
    return el()
  }

  fun <T> go(url: String, fn: El.() -> T): T {
    driver.get(url)
    return el().fn()
  }

  fun el(): El {
    return El(jq = this, selector = listOf())
  }

  override fun close() {
    driver.close()
    driver.quit()
  }
}
