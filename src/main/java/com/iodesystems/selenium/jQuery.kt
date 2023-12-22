package com.iodesystems.selenium

import org.openqa.selenium.*
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.RemoteWebElement
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.Select
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration


data class jQuery(
  val driver: RemoteWebDriver,
  val timeout: Duration = Duration.ofSeconds(5),
  val logQueriesToBrowser: Boolean = false,
  val logQueriesToStdout: Boolean = false
) {
  class RetryException(
    message: String, cause: Throwable? = null
  ) : Exception(message, cause)

  @Suppress("unused")
  data class Either(
    private val el: IEl, private val left: Boolean
  ) : IEl by el {
    fun <T> left(fn: IEl.() -> T): T? {
      return if (left) fn(el)
      else null
    }

    fun <T> right(fn: IEl.() -> T): T? {
      return if (left) null
      else fn(el)
    }
  }

  interface IEl {
    fun actions(): Actions
    fun atLeast(): Int?
    fun atMost(): Int?
    fun click(): IEl
    fun clear(): IEl
    fun blur(): IEl
    fun text(): String
    fun sendKeys(text: CharSequence, rateMillis: Int? = null): IEl
    fun contains(text: String): IEl
    fun value(): String
    fun gone()
    fun exists()
    fun visible(): IEl
    fun ensureEnabled(): IEl
    fun ensureDisabled(): IEl
    fun maybeExists(): Boolean
    fun <T> ifExists(fn: IEl.() -> T): T?
    fun element(): RemoteWebElement
    fun elementsUnChecked(): List<RemoteWebElement>
    fun elements(): List<RemoteWebElement>
    fun renderScript(): String
    fun renderSelector(): String
    fun scrollIntoView(): IEl
    fun screenshot(destinationFile: String): IEl

    // Generic child finder
    fun findAll(childSelector: String, atLeast: Int? = 1, atMost: Int? = null): IEl
    fun findAll(childSelector: List<String>, atLeast: Int? = 1, atMost: Int? = null): IEl

    // Single child finders
    fun <T> find(childSelector: List<String>, fn: IEl.() -> T): T
    fun <T> find(childSelector: String, fn: IEl.() -> T): T
    fun find(childSelector: List<String>): IEl
    fun find(childSelector: String): IEl
    fun require(atMost: Int?, atLeast: Int?): IEl

    // Parent finders
    fun parent(parentSelector: String, atLeast: Int? = 1, atMost: Int? = 1): IEl
    fun parent(parentSelector: List<String>, atLeast: Int? = 1, atMost: Int? = 1): IEl


    fun either(left: String, right: String): Either
    fun either(left: IEl, right: IEl): Either
    fun first(first: IEl, vararg rest: IEl): IEl?
    fun escape(string: String): String
    fun enabled(): IEl
    fun first(): IEl
    fun selectValue(value: String): IEl
    fun last(): IEl
    fun reroot(selector: String? = null): IEl
    fun <T> withFrame(selector: String, fn: IEl.() -> T): T
    fun waitUntil(message: String = "condition to be true", fn: IEl.() -> Boolean): IEl
    fun <T> waitFor(message: String = "expression to be nonnull", fn: IEl.() -> T?): T
    fun js(script: String, vararg args: Any?): Any?
    fun jq(): jQuery
  }

  data class El(
    val jq: jQuery,
    val selector: List<String>,
    val atLeast: Int? = null,
    val atMost: Int? = null,
  ) : IEl {

    private fun <T> safely(
      el: RemoteWebElement,
      fn: RemoteWebElement.() -> T
    ): T {
      try {
        return fn(el)
      } catch (e: ElementNotInteractableException) {
        throw RetryException("Element not interactable", e)
      } catch (e: StaleElementReferenceException) {
        throw RetryException("Stale element reference", e)
      } catch (e: MoveTargetOutOfBoundsException) {
        try {
          Actions(jq.driver as WebDriver).moveToElement(el).perform()
        } catch (e: MoveTargetOutOfBoundsException) {
          scrollIntoView()
          throw RetryException("Element cannot be scrolled to", e)
        }
        throw RetryException("Element requires scrolling to", e)
      }
    }

    override fun actions(): Actions {
      return Actions(jq.driver)
    }

    override fun atLeast(): Int? {
      return atLeast
    }

    override fun atMost(): Int? {
      return atMost
    }

    override fun click(): IEl {
      jq.waitFor("could not click") {
        val element = element()
        try {
          try {
            safely(element) {
              click()
            }
          } catch (e: ElementClickInterceptedException) {
            jq.driver.executeScript("arguments[0].click()", element)
          }
        } catch (e: StaleElementReferenceException) {
          throw RetryException("Stale element", e)
        }
      }
      return this
    }

    override fun clear(): IEl {
      jq.waitFor("could not clear element") {
        safely(element()) {
          clear()
          val length = getAttribute("value").length
          if (length > 0) sendKeys((0..(getAttribute("value").length)).joinToString("") {
            Keys.BACK_SPACE
          })
        }
      }
      return this
    }

    override fun blur(): IEl {
      actions().sendKeys(Keys.TAB).perform()
      return this
    }

    override fun text(): String {
      return element().text
    }

    override fun sendKeys(text: CharSequence, rateMillis: Int?): IEl {
      if (rateMillis == null) {
        jq.waitFor("Could not send keys") {
          safely(element()) {
            sendKeys(text)
          }
        }
      } else {
        val script = renderScript()
        jq.waitFor(
          "Could not send keys",
          retry = Duration.ofMillis(text.length * rateMillis.toLong()),
          timeOut = Duration.ofMillis(text.length * rateMillis.toLong() * 100),
        ) {
          val elements = elementsUnChecked()
          if (elements.size != 1) {
            throw RetryException("Elements for $script not 1, but ${elements.size}")
          }
          safely(elements[0]) {
            text.fold(
              Actions(jq.driver as WebDriver)
                .click(this)
            ) { a, c ->
              a.pause(Duration.ofMillis(rateMillis.toLong())).sendKeys(c.toString())
            }.perform()
          }
        }
      }
      return this
    }

    override fun contains(text: String): IEl {
      val textEncoded = jq.escape(text)
      return copy(selector = selector.map { "$it:contains($textEncoded)" })
    }

    override fun value(): String {
      return element().getAttribute("value")
    }

    override fun gone() {
      copy(atMost = 0, atLeast = null).elements()
    }

    override fun exists() {
      element()
    }

    override fun ensureEnabled(): El {
      copy(
        selector = selector(":enabled")
      ).element()
      return this
    }

    override fun ensureDisabled(): El {
      copy(
        selector = selector(":disabled")
      ).element()
      return this
    }

    fun selector(extension: String? = null): List<String> {
      return if (extension != null)
        selector.take(selector.size - 1) +
            (selector.last() + extension)
      else
        selector
    }

    override fun visible(): IEl {
      return copy(
        selector = selector(":visible")
      )
    }

    override fun maybeExists(): Boolean {
      return elementsUnChecked().isNotEmpty()
    }

    override fun <T> ifExists(fn: IEl.() -> T): T? {
      return if (maybeExists()) return null
      else fn(this)
    }

    override fun element(): RemoteWebElement {
      return elements().first()
    }

    override fun elementsUnChecked(): List<RemoteWebElement> {
      return jq.search(
        listOf(this.copy(atLeast = null, atMost = null))
      ).firstOrNull() ?: emptyList()
    }

    override fun elements(): List<RemoteWebElement> {
      return jq.search(listOf(this)).first()!!
    }

    override fun renderSelector(): String {
      return selector.joinToString(", ")
    }

    override fun js(script: String, vararg args: Any?): Any? {
      return jq.driver.executeScript(script, *args)
    }

    override fun jq(): jQuery {
      return jq
    }

    override fun scrollIntoView(): IEl {
      jq.driver.executeScript(
        "arguments[0].scrollIntoView({block:'end', inline:'end', behavior:'instant'})",
        element()
      )
      return this
    }

    override fun screenshot(destinationFile: String): IEl {
      File(destinationFile).writeBytes(jq.driver.getScreenshotAs(OutputType.BYTES))
      return this
    }

    override fun renderScript(): String {
      return """
                jQuery(${escape(renderSelector())})
            """.trimIndent()
    }

    override fun findAll(childSelector: List<String>, atLeast: Int?, atMost: Int?): IEl {
      return copy(
        selector = selector.map { parent ->
          childSelector.map { child ->
            "$parent $child".trim()
          }
        }.flatten(),
        atLeast = atLeast,
        atMost = atMost,
      )
    }

    override fun findAll(childSelector: String, atLeast: Int?, atMost: Int?): IEl {
      return findAll(listOf(childSelector), atLeast, atMost)
    }

    override fun <T> find(childSelector: List<String>, fn: IEl.() -> T): T {
      return find(childSelector).run(fn)
    }

    override fun find(childSelector: List<String>): IEl {
      return findAll(childSelector, 1, 1)
    }

    override fun find(childSelector: String): IEl {
      return find(listOf(childSelector))
    }

    override fun require(atMost: Int?, atLeast: Int?): IEl {
      return copy(atMost = atMost, atLeast = atLeast)
    }

    override fun <T> find(childSelector: String, fn: IEl.() -> T): T {
      return fn(find(childSelector))
    }

    override fun parent(parentSelector: String, atLeast: Int?, atMost: Int?): IEl {
      return parent(listOf(parentSelector), atLeast, atMost)
    }

    override fun parent(parentSelector: List<String>, atLeast: Int?, atMost: Int?): IEl {
      return copy(
        selector = parentSelector.map { parent ->
          selector.map { child ->
            "$parent:has(${child}):last"
          }
        }.flatten(),
        atLeast = atLeast,
        atMost = atMost
      )
    }

    override fun enabled(): IEl {
      return copy(selector = selector.map { "$it:enabled" })
    }

    override fun reroot(selector: String?): IEl {
      return copy(
        selector = if (selector != null) listOf(selector) else emptyList(),
        atLeast = 1,
        atMost = null
      )
    }

    override fun escape(string: String): String {
      return jq.escape(string)
    }

    override fun first(first: IEl, vararg rest: IEl): IEl? {
      val all = listOf(first) + rest.toList()
      val results = jq.search(all)
      val found = results.find { it != null }
      if (found != null) {
        return all[results.indexOf(found)]
      }
      return null
    }

    override fun first(): IEl {
      return copy(selector = selector.map { "$it:first" }, atLeast = 1, atMost = null)
    }

    override fun last(): IEl {
      return copy(selector = selector.map { "$it:last" }, atLeast = 1, atMost = null)
    }

    override fun selectValue(value: String): IEl {
      Select(element()).selectByValue(value)
      return this
    }

    override fun <T> withFrame(selector: String, fn: IEl.() -> T): T {
      val dr = (jq.driver as WebDriver)
      val frame = find(selector).element()
      dr.switchTo().frame(frame)
      val result = fn(copy(selector = listOf("")))
      dr.switchTo().defaultContent()
      return result
    }

    override fun waitUntil(message: String, fn: IEl.() -> Boolean): IEl {
      val msg = "Timeout waiting for $message on ${renderScript()}"
      jq.waitFor(msg) {
        if (!fn(this)) {
          throw RetryException(msg)
        }
      }
      return this
    }

    override fun <T> waitFor(message: String, fn: IEl.() -> T?): T {
      return jq.waitFor(message) {
        fn(this) ?: throw RetryException("Timeout waiting for $message on ${renderScript()}")
      }
    }


    override fun either(
      left: String, right: String
    ): Either {
      return either(find(left), find(right))
    }

    override fun either(left: IEl, right: IEl): Either {
      return jq.waitFor("Either left or right not found, or both found") {
        val results = jq.search(listOf(left, right))
        val leftElements = results[0] ?: emptyList()
        val rightElements = results[1] ?: emptyList()
        if (leftElements.size == 1) {
          Either(left, left = true)
        } else if (rightElements.size == 1) {
          Either(right, left = false)
        } else {
          val script = """
                        left: ${left.renderScript()}
                        right: ${right.renderScript()}

                    """.trimIndent()
          if (leftElements.isEmpty()) {
            throw RetryException(
              "Either found no elements:\n${script}"
            )
          } else {
            throw RetryException(
              "Either found both elements:\n${script}"
            )
          }

        }
      }
    }
  }

  fun install() {
    mapOf(
      "jQuery" to "jquery-3.6.3.min.js",
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
      }
    }
  }

  fun escape(string: String): String {
    return '"' + string.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + '"'
  }

  fun search(els: List<IEl>): List<List<RemoteWebElement>?> {
    return waitFor("Search query to return stable results") {
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
        throw RetryException("Script timeout", e)
      }
    }
  }

  fun <T> waitFor(
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

    fun <T> page(url: String, fn: El.() -> T): T {
        driver.get(url)
        return fn(find())
    }

    fun find(
        selector: String = "html",
        atLeast: Int? = 1,
        atMost: Int? = null,
    ): El = find(listOf(selector), atLeast, atMost)

    fun find(
        selector: List<String>,
        atLeast: Int? = 1,
        atMost: Int? = null,
    ): El {
        return El(
            jq = this, selector = selector, atLeast = atLeast, atMost = atMost
        )
    }
}
