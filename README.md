Selenium jQuery
====================================
Working with selenium's selector api is a huge pain in the ass.

What technology likes to fix bad web-centric apis for the most developer centric ergonomics? JQUERY!

The technology from the 90s is still saving us from bad design today!

```xml
<dependency>
  <groupId>com.iodesystems.selenium-jquery</groupId>
  <artifactId>selenium-jquery</artifactId>
  <version>2.1.3</version>
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
// Use WebDriverManager to ensure full selenium protocol compatibility for performance
WebDriverManager.chromedriver().setup()
// Silence as much noise as possible
val chromeDriverService: ChromeDriverService = ChromeDriverService.Builder()
    .withSilent(true)
    .build()
chromeDriverService.sendOutputTo(NullOutputStream.NULL_OUTPUT_STREAM)

var options = ChromeOptions()
// Disable attempts to save things to the profile
options.setExperimentalOption(
    "prefs", mapOf(
        "autofill.profile_enabled" to false
    )
)
// Don't show popup notifications
options.addArguments("--disable-notifications")
// Ignore prompts
options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.DISMISS)
// Don't bother waiting for the entire dom to load, if you can do your thang already
options.setPageLoadStrategy(PageLoadStrategy.EAGER)
if (headless) {
    options = options
        .addArguments("--headless")
        .addArguments("window-size=1920,1080")
}
val driver = ChromeDriver(chromeDriverService, options)
// Set some sane amount of timeout so tests don't hang on issues
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5))
```

Using the `jQuery` object:
```kotlin
jq.page("http://google.com") {
    find("input[name='q']").sendKeys("hello world", 30)
    find("input[value='Google Search']").first().click()
    find("#result-stats").visible()
}
```

Nested objects, parent and child traversals:
```kotlin
jq.page("http://some-page.com") {
    frame("#frame-id") {
        find(".hidden-in-frame").parent(".most-recent-parent-containing") {
            find(".child-of-that-parent")
        }
    }
    find(".a"){
        // Hierarchy can be nested as far as you like
        find(".b"){
            // Reroot breaks out of hierarchy            
            reroot("body").exists()
        }
    }
}
```

More aggressive testing for difficult controls:
```kotlin
// Not direct clicking due to bubbling? FORCE IT!
find(".weird-control").clickForce()
// Controlled text input masking things all weird? Put a delay in it!
find(".text-settling-input").clear().sendKeys("important", delayMilis = 100)
```

Extension
------------------------------------
I'll admit, `SeleniumJQuery` doesn't have it all, pull requests are welcome, however, it's super easy to
extend the `IEl` object with `kotlin`'s extension functions:

```kotlin
// Some examples for the Mui framework and general time savers:
fun jQuery.IEl.inputLabeled(label: String) =
  find(".MuiInputBase-formControl")
      .contains(label)
      .find(":input:first")
fun jQuery.IEl.buttonLabeled(label: String) = 
    find("button")
    .contains(label)
    .enabled()
fun jQuery.IEl.linkWithText(text: String) = 
    find("a")
        .contains(text)
fun jQuery.IEl.buttonWithIcon(icon: String) = 
    find("svg[data-testid='$icon']")
        .parent("button")
```

Custom domain objects are also a breeze:
```kotlin
// Page Object DSL
data class Dialog(val el:IEl) : IEl by el {
    fun password(password:String) = inputLabeled("Password").first().sendKeys(password)
    fun submit() = find("button[type=submit]").click()
}
// Bootstrap extension:
fun <T> jQuery.IEl.dialog(
    label: String,
    fn: Dialog.()->T
) {
    val el = find(".dialog-container").contains(label)
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
