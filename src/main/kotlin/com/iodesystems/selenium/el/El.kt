package com.iodesystems.selenium.el

import com.iodesystems.selenium.exceptions.RetryException
import com.iodesystems.selenium.jQuery
import org.openqa.selenium.*
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.RemoteWebElement
import org.openqa.selenium.support.ui.Select
import java.io.File
import java.time.Duration

data class El(
  override val jq: jQuery,
  override val selector: List<String>,
  val atLeast: Int? = null,
  val atMost: Int? = null,
) : IEl {

  private fun <T> safely(
    el: RemoteWebElement,
    fn: RemoteWebElement.() -> T
  ): T {
    try {
      return fn(el)
    } catch (e: InvalidElementStateException) {
      throw RetryException("Element not interactable", e)
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

  override fun data(key: String): String? {
    return element().getAttribute("data-$key")
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

  override fun one(): IEl {
    return require(1, 1)
  }

  override fun click(): IEl {
    jq.waitForNonNull("could not click") {
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
    jq.waitForNonNull("could not clear element") {
      safely(element()) {
        clear()
        val length = (getAttribute("value") ?: "").length
        if (length > 0) sendKeys((0..length).joinToString("") {
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
      jq.waitForNonNull("Could not send keys") {
        safely(element()) {
          sendKeys(text)
        }
      }
    } else {
      val script = renderScript()
      jq.waitForNonNull(
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

  override fun refine(refineSelector: String): IEl {
    return copy(
      selector =
        if (selector.isEmpty()) listOf(refineSelector)
        else selector.map { "$it$refineSelector" }
    )
  }


  override fun contains(text: String): IEl {
    return refine(":contains(${jq.escape(text)})")
  }

  override fun value(): String {
    return (element().getAttribute("value") ?: "").trim()
  }

  override fun gone() {
    copy(atMost = 0, atLeast = null).elements()
  }

  override fun exists() {
    if (atLeast == null || atLeast == 0) {
      copy(atLeast = 1).element()
    } else element()
  }

  override fun disabled(): IEl {
    return refine(":disabled")
  }

  override fun visible(): IEl {
    return refine(":visible")
  }

  override fun maybeExists(): Boolean {
    return elementsUnChecked().isNotEmpty()
  }

  override fun <T> ifExists(fn: IEl.() -> T): T? {
    return if (maybeExists()) return null
    else fn(this)
  }

  override fun withDriver(remoteWebDriver: RemoteWebDriver): IEl {
    return jq.copy(driver = remoteWebDriver).root()
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
    return if (selector.isEmpty()) ":root"
    else selector.joinToString(", ")
  }

  override fun js(script: String, vararg args: Any?): Any? {
    return jq.driver.executeScript(script, *args)
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
      selector = if (selector.isEmpty()) childSelector else selector.map { parent ->
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
    return refine(":enabled")
  }

  override fun reroot(selector: String?): IEl {
    return copy(
      selector = if (selector == null) emptyList() else listOf(selector),
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
    return copy(atLeast = 1, atMost = null).refine(":first")
  }

  override fun last(): IEl {
    return copy(atLeast = 1, atMost = null).refine(":last")
  }

  override fun selectValue(value: String): IEl {
    Select(element()).selectByValue(value)
    return this
  }

  override fun <T> withTab(label: String, cb: IEl.() -> T): T {
    val jq = jq
    val driver = waitFor {
      jq.driver.windowHandles.stream().map { handle ->
        val newDriver = jq.driver.switchTo().window(handle)
        val title = newDriver.title ?: ""
        if (title.contains(label, ignoreCase = true)) {
          newDriver
        } else {
          newDriver.close()
          null
        }
      }.filter { it != null }.findFirst().orElse(null)
    } as RemoteWebDriver
    try {
      return cb(jq.copy(driver = driver).root())
    } finally {
      driver.close()
    }
  }

  override fun <T> withFrame(selector: String, fn: IEl.() -> T): T {
    val dr = (jq.driver as WebDriver)
    val frame = find(selector).element()
    val driver = dr.switchTo().frame(frame) as RemoteWebDriver
    try {
      return fn(jq.copy(driver = driver).root())
    } finally {
      driver.close()
    }
  }

  override fun waitUntil(message: String, fn: IEl.() -> Boolean): IEl {
    val msg = "Timeout waiting for $message on ${renderScript()}"
    jq.waitForNonNull(msg) {
      if (!fn(this)) {
        throw RetryException(msg)
      }
    }
    return this
  }

  override fun <T> waitFor(message: String, fn: IEl.() -> T?): T {
    return jq.waitForNonNull(message) {
      fn(this) ?: throw RetryException("Timeout waiting for $message on ${renderScript()}")
    }
  }


  override fun either(
    left: String, right: String
  ): Either {
    return either(find(left), find(right))
  }

  override fun either(left: IEl, right: IEl): Either {
    return jq.waitForNonNull("Either left or right not found, or both found") {
      val results = jq.search(listOf(left, right))
      val leftElements = results[0] ?: emptyList()
      val rightElements = results[1] ?: emptyList()
      if (leftElements.isEmpty() && rightElements.isEmpty()) {
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
      Either(
        left = if (leftElements.size == 1) left else null,
        right = if (rightElements.size == 1) right else null
      )
    }
  }
}
