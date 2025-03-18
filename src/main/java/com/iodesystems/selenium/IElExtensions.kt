package com.iodesystems.selenium

import com.iodesystems.selenium.jQuery.IEl
import org.openqa.selenium.remote.RemoteWebDriver

object IElExtensions {
  fun IEl.button(text: String): IEl {
    return find("button").icontains(text)
  }

  fun IEl.link(text: String): IEl {
    return find("a").icontains(text)
  }

  fun IEl.linkTo(href: String): IEl {
    return find("a[href='$href']")
  }

  fun IEl.waitForPageLoaded() {
    waitFor("document.readyState === 'complete'") {
      if (js("return document.readyState") == "complete") true else null
    }
  }

  fun IEl.not(selector: String): IEl {
    return refine(":not($selector)")
  }


}
