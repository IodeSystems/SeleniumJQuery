package com.anteambulo.SeleniumJQuery.example;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.anteambulo.SeleniumJQuery.jQuery;
import com.anteambulo.SeleniumJQuery.jQueryBrowser;

public class GooglePlus extends jQueryBrowser {
  public GooglePlus(RemoteWebDriver drv, String username, String password) {
    super(drv);
  }

  public static void main(String[] args) throws TimeoutException, IOException {
    if (args.length != 3) {
      System.out.println("usage: java -classpath SeleniumJQuery.jar " + GooglePlus.class.getName());
      return;
    }

    FirefoxBinary bin = new FirefoxBinary(new File("/home/ctaylor/local/inst/firefox-5.0/firefox-bin"));
    FirefoxProfile prof = new FirefoxProfile();
    prof.addExtension(new File("/home/carl/local/inst/firebug-1.8.0.xpi"));
    prof.setPreference("extensions.firebug.currentVersion", "1.8.0");
    prof.setPreference("extensions.firebug.console.enableSites", true);
    prof.setPreference("extensions.firebug.net.enableSites", true);
    prof.setPreference("extensions.firebug.script.enableSites", true);

    RemoteWebDriver drv = new FirefoxDriver(bin, prof);
    try {
      GooglePlus gp = new GooglePlus(drv, args[0], args[1]);
      gp.stream(args[2]);
    } finally {
      drv.quit();
    }
  }

  public void login() throws TimeoutException {
    try {
      querySafe("a:contains(Sign in):last:visible").get().click();
      WebElement email = queryUntil("#Email").clear().get();
      email.sendKeys("");
      WebElement pw = queryUntil("#Passwd").clear().get();
      pw.sendKeys("");
      queryUntil("#signIn").get().click();
    } catch (NoSuchElementException e) {
      // Already logged in?
    }
  }

  public void stream(String msg) throws TimeoutException {
    get("https://plus.google.com/");
    login();
    jQuery share = queryUntil(":contains(Share what's new...)");
    share.click();
    queryUntil("div.editable:visible").get().sendKeys(msg);
    queryUntil("tr div:contains(Share):last").click();
    System.out.println();
  }
}
