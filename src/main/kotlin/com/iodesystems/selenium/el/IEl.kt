package com.iodesystems.selenium.el

import com.iodesystems.selenium.jQuery
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.RemoteWebElement

interface IEl {
  fun data(key: String): String?
  fun attr(key: String): String?

  fun actions(): Actions
  fun atLeast(): Int?
  fun atMost(): Int?
  fun one(): IEl
  fun click(): IEl
  fun clear(): IEl
  fun blur(): IEl
  fun text(): String
  fun sendKeys(text: CharSequence, rateMillis: Int? = null): IEl
  fun withDriver(remoteWebDriver: RemoteWebDriver): IEl
  fun refine(refineSelector: String): IEl
  fun contains(text: String): IEl
  fun value(): String
  fun gone()
  fun exists()
  fun visible(): IEl
  fun enabled(): IEl
  fun disabled(): IEl
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

  fun first(first: String, vararg rest: String): IEl
  fun first(first: IEl, vararg rest: IEl): IEl
  fun escape(string: String): String

  fun first(): IEl
  fun selectValue(value: String): IEl
  fun last(): IEl
  fun reroot(selector: String? = null): IEl
  fun <T> withTab(label: String, cb: IEl.() -> T): T
  fun <T> withFrame(selector: String, fn: IEl.() -> T): T
  fun waitUntil(message: String = "condition to be true", fn: IEl.() -> Boolean): IEl
  fun <T> waitFor(message: String = "expression to be nonnull", fn: IEl.() -> T?): T
  fun js(script: String, vararg args: Any?): Any?

  val jq: jQuery
  val selector: List<String>

}
