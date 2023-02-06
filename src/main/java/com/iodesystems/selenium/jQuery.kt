package com.iodesystems.selenium

import org.openqa.selenium.ElementClickInterceptedException
import org.openqa.selenium.JavascriptException
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.internal.Either
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.RemoteWebElement
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.Select
import java.io.ByteArrayOutputStream
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

    interface IEl {
        fun click(force: Boolean = false): IEl
        fun clear(force: Boolean = false): IEl
        fun text(): String
        fun sendKeys(text: CharSequence, rateMillis: Int? = null): IEl
        fun contains(text: String): IEl
        fun value(): String
        fun gone()
        fun exists()
        fun maybeExists(): Boolean
        fun element(): RemoteWebElement
        fun elementsUnChecked(): List<RemoteWebElement>
        fun elements(): List<RemoteWebElement>
        fun renderScript(): String
        fun findAll(childSelector: String, atLeast: Int? = 1, atMost: Int? = null): IEl
        fun find(childSelector: String): IEl
        fun <T> find(childSelector: String, fn: IEl.() -> T): T
        fun parent(parentSelector: String): IEl
        fun <T> parent(parentSelector: String, fn: IEl.() -> T): T
        fun parents(parentSelector: String, atLeast: Int? = 1, atMost: Int? = null): IEl
        fun either(left: String, right: String): Either<IEl, IEl>
        fun enabled(): IEl
        fun first(): IEl
        fun selectValue(value: String): IEl
        fun last(): IEl
        fun reroot(selector: String): IEl
        fun <T> withFrame(selector: String, fn: IEl.() -> T): T
        fun waitUntil(message: String? = null, fn: IEl.() -> Boolean): IEl
        fun <T> waitFor(message: String? = null, fn: IEl.() -> T?): T
    }

    data class El(
        val jq: jQuery,
        val selector: List<String>,
        val atLeast: Int? = null,
        val atMost: Int? = null,
    ) : IEl {

        override fun click(force: Boolean): IEl {
            try {
                element().click()
            } catch (e: ElementClickInterceptedException) {
                if (force) {
                    jq.driver.executeScript("arguments[0].click()", element())
                } else throw e

            }
            return this
        }

        override fun clear(force: Boolean): IEl {
            val el = element()
            el.clear()
            if (force) {
                el.sendKeys((0..(el.getAttribute("value").length))
                    .joinToString("") {
                        Keys.BACK_SPACE
                    })
            }
            return this
        }

        override fun text(): String {
            return element().text
        }

        override fun sendKeys(text: CharSequence, rateMillis: Int?): IEl {
            if (rateMillis == null) {
                element().sendKeys(text)
            } else {
                jq.waitFor(
                    "Could not send keys",
                    retry = Duration.ofMillis(text.length * rateMillis.toLong()),
                    timeOut = Duration.ofMillis(text.length * rateMillis.toLong() * 100),
                ) {
                    val elements = elementsUnChecked()
                    if (elements.size != 1) {
                        throw RetryException("Elements for $selector not 1, but ${elements.size}")
                    }
                    val actions = Actions(jq.driver as WebDriver).click(elements[0])
                    val actionWithKeys = text.fold(actions) { a, c ->
                        a.pause(Duration.ofMillis(rateMillis.toLong())).sendKeys(c.toString())
                    }
                    try {
                        actionWithKeys.perform()
                    } catch (e: Exception) {
                        throw RetryException("Could not interact", e)
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
            copy(atMost = null, atLeast = 1).elements()
        }

        override fun maybeExists(): Boolean {
            return copy(atMost = null, atLeast = null).elements().isNotEmpty()
        }

        override fun element(): RemoteWebElement {
            return elements()[0]
        }

        override fun elementsUnChecked(): List<RemoteWebElement> {
            val script = renderScript()
            val execute = if (jq.logQueriesToBrowser) {
                val scriptEncoded = jq.escape(script)
                """
                  console.log($scriptEncoded);
                  return $script
                """.trimIndent()
            } else {
                """
                  return $script
                """.trimIndent()
            }
            if (jq.logQueriesToStdout) {
                println(script)
            }
            @Suppress("UNCHECKED_CAST") return jq.driver.executeScript(execute) as List<RemoteWebElement>
        }

        override fun elements(): List<RemoteWebElement> {
            return jq.waitFor("Elements") {
                val elements = elementsUnChecked()
                if (atLeast != null) {
                    if (elements.size < atLeast) {
                        throw RetryException("Not enough elements found looking for: \n${renderScript()}")
                    }
                }
                if (atMost != null) {
                    if (elements.size > atMost) {
                        throw RetryException("Too many elements found looking for: \n${renderScript()}")
                    }
                }
                elements
            }
        }

        override fun renderScript(): String {
            val encoded = jq.escape(selector.joinToString(", "))
            return """
                jQuery($encoded)
            """.trimIndent()
        }

        override fun findAll(childSelector: String, atLeast: Int?, atMost: Int?): IEl {
            return copy(
                selector = selector.map { "$it $childSelector".trim() },
                atLeast = atLeast,
                atMost = atMost,
            )
        }

        override fun find(childSelector: String): IEl {
            return copy(
                selector = selector.map { "$it $childSelector".trim() }, atLeast = 1, atMost = 1
            )
        }

        override fun <T> find(childSelector: String, fn: IEl.() -> T): T {
            return fn(find(childSelector))
        }

        override fun parent(parentSelector: String): IEl {
            return parents(parentSelector, 1, 1)
        }

        override fun <T> parent(parentSelector: String, fn: IEl.() -> T): T {
            return fn(parent(parentSelector))
        }

        override fun parents(parentSelector: String, atLeast: Int?, atMost: Int?): IEl {
            return copy(
                selector = listOf("$parentSelector:has(${selector.joinToString(",")}):last"),
                atLeast = atLeast,
                atMost = atMost
            )
        }

        override fun enabled(): IEl {
            return copy(selector = selector.map { "$it:enabled" })
        }

        override fun reroot(selector: String): IEl {
            return copy(selector = listOf(selector), atLeast = 1, atMost = null)
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
            jq.install()
            val result = fn(copy(selector = listOf("")))
            dr.switchTo().defaultContent()
            return result
        }

        override fun waitUntil(message: String?, fn: IEl.() -> Boolean): IEl {
            val message = message ?: "provided condition was not true"
            jq.waitFor(message) {
                if (!fn(this)) {
                    throw RetryException("Timeout while $message")
                }
            }
            return this
        }

        override fun <T> waitFor(message: String?, fn: IEl.() -> T?): T {
            val message = message ?: "no value returned"
            return jq.waitFor(message) {
                fn(this) ?: throw RetryException("Timeout while $message")
            }
        }


        override fun either(
            left: String, right: String
        ): Either<IEl, IEl> {
            return jq.waitFor("Either left or right not found, or both found") {
                val leftEl = find(left)
                val rightEl = find(right)
                val leftElements = leftEl.elementsUnChecked()
                val rightElements = rightEl.elementsUnChecked()
                if (leftElements.size == 1) {
                    Either.left(leftEl)
                } else if (rightElements.size == 1) {
                    Either.right(rightEl)
                } else {
                    throw RetryException("Either no elements found, or both elements found")
                }
            }
        }
    }

    fun install() {
        if (driver.executeScript("return typeof window.jQuery") == "undefined") {
            val jQueryStream = javaClass.getResourceAsStream("/jquery-3.6.3.min.js")
            val jQueryStreamBuffer = ByteArrayOutputStream()
            jQueryStream?.transferTo(jQueryStreamBuffer)
            val jQueryContent = jQueryStreamBuffer.toString()
            driver.executeScript(jQueryContent)
        }
    }

    fun escape(string: String): String {
        return '"' + string.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + '"'
    }

    fun <T> waitFor(
        failureMessage: String, retry: Duration? = null, timeOut: Duration? = null, t: () -> T
    ): T {
        return FluentWait(driver).withTimeout(timeOut ?: timeout).pollingEvery(retry ?: Duration.ofMillis(100))
            .withMessage(failureMessage).ignoring(RetryException::class.java).until {
                try {
                    t()
                } catch (e: JavascriptException) {
                    install()
                    t()
                }
            }
    }

    fun <T> page(fn: El.() -> T): T {
        return fn(find("", null))
    }

    fun <T> page(url: String, fn: El.() -> T): T {
        driver.get(url)
        return page(fn)
    }

    fun find(
        selector: String,
        atLeast: Int? = 1,
        atMost: Int? = null,
    ): El {
        return El(
            jq = this, selector = listOf(selector), atLeast = atLeast, atMost = atMost
        )
    }
}
