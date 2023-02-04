Selenium jQuery
====================================
Working with selenium's selector api is a huge pain in the ass.

What technology likes to fix bad web-centric apis for the most developer centric ergonomics? JQUERY!

The technology from the 90s is still saving us from bad design today!

```xml
<dependency>
  <groupId>com.iodesystems.selenium-jquery</groupId>
  <artifactId>selenium-jquery</artifactId>
  <version>2.0.1</version>
</dependency>
```

Overview
------------------------------------
`SeleniumJQuery` rides on `Selenium`'s `RemoteWebDriver` api and automagically installs `jQuery` in 
websites to make automation and testing a breeze.

Combined with `kotlin`'s DSL utilities, `PageObject` design goes to it's well deserved grave.

Example
------------------------------------

Check out `jQueryTest.kt` for an example of how to bootstrap the `jQuery` object
using `webdrivermanager`:
```xml
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.3.2</version>
    <scope>test</scope>
</dependency>
```

Creating the driver and `jQuery` instance:
```kotlin
// Use
val chromeDriverService: ChromeDriverService = ChromeDriverService.Builder().build()
// Suppress pesky, pointless messages
chromeDriverService.sendOutputTo(NullOutputStream.NULL_OUTPUT_STREAM)
val driver = ChromeDriver(chromeDriverService, ChromeOptions())
val jq = jQuery(
    driver,
    // helpful for testing development:
    logToBrowser=true,
    logToStdout=true
)
```

Using the `jQuery` object:
```kotlin
jq.page("http://google.com") {
    child("input[name='q']").sendKeys("hello world", 30)
    child("input[value='Google Search']").first().click()
    child("#result-stats").exists()
}
```

Nested objects, parent and child traversals:
```kotlin
jq.page("http://some-page.com") {
    frame("#frame-id") {
        child(".hidden-in-frame").parent(".most-recent-parent-containing") {
            child(".child-of-that-parent")
        }
    }
    child(".a"){
        // Hierarchy can be nested as far as you like
        child(".b"){
            // Reroot breaks out of hierarchy            
            reroot("body").exists()
        }
    }
}
```

More aggressive testing for difficult controls:
```kotlin
// Not direct clicking due to bubbling? FORCE IT!
child(".weird-control").clickForce()
// Controlled text input masking things all weird? Put a delay in it!
child(".text-settling-input").clear().sendKeys("important", delayMilis = 100)
```

Extension
------------------------------------
I'll admit, `SeleniumJQuery` doesn't have it all, pull requests are welcome, however, it's super easy to
extend the `IEl` object with `kotlin`'s extension functions:

```kotlin
// Some examples for the Mui framework and general time savers:
fun jQuery.IEl.inputLabeled(label: String) =
  child(".MuiInputBase-formControl")
      .contains(label)
      .child(":input:first")
fun jQuery.IEl.buttonLabeled(label: String) = 
    child("button")
    .contains(label)
    .enabled()
fun jQuery.IEl.linkWithText(text: String) = 
    child("a")
        .contains(text)
fun jQuery.IEl.buttonWithIcon(icon: String) = 
    child("svg[data-testid='$icon']")
        .parent("button")
```

Custom domain objects are also a breeze:
```kotlin
// Page Object DSL
data class Dialog(val el:IEl) : IEl by el {
    fun password(password:String) = inputLabeled("Password").first().sendKeys(password)
    fun submit() = child("button[type=submit]").click()
}
// Bootstrap extension:
fun <T> jQuery.IEl.dialog(
    label: String,
    fn: Dialog.()->T
) {
    val el = child(".dialog-container").contains(label)
    return fn(Dialog(el))
}
// Usage:
...
jq.page{
    dialog("Enter Password"){
        password("super secure!") // DSL
        submit() // DSL
        gone() // EL method: Wait until dialog disappears
    }
}
...
```
