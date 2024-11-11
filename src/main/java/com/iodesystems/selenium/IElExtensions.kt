package com.iodesystems.selenium

import com.iodesystems.selenium.jQuery.IEl

object IElExtensions {
  fun IEl.button(text: String): IEl {
    return find("button").contains(text)
  }

  fun IEl.link(text: String): IEl {
    return find("a").contains(text)
  }

  fun IEl.linkTo(href: String): IEl {
    return find("a[href='$href']")
  }

  fun IEl.waitForPageLoaded() {
    waitFor("document.readyState === 'complete'") {
      if (js("return document.readyState") == "complete") true else null
    }
  }
}
