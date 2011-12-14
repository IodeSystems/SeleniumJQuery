package com.anteambulo.SeleniumJQuery.example;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.anteambulo.SeleniumJQuery.jQuery;
import com.anteambulo.SeleniumJQuery.jQueryBrowser;

public class GooglePlus extends jQueryBrowser {
  private String password;
  private String username;

  public GooglePlus(RemoteWebDriver drv, String username, String password) {
    super(drv);
    this.username = username;
    this.password = password;
  }

  public static void main(String[] args) throws TimeoutException, IOException {
    if (args.length != 3) {
      System.out.println("usage: java -classpath SeleniumJQuery.jar " + GooglePlus.class.getName() + " username password 'your message'");
      System.out.println("You can specify a compatible firefox-bin with -Dwebdriver.firefox.bin=/path/to/firefox-bin");
      System.out.println("You can specify loading firebug with -Dwebdriver.firefox.firebug=/path/to/firebug.xpi");
      return;
    }
    FirefoxBinary bin;
    try {
      bin = new FirefoxBinary();
    } catch (WebDriverException e) {
      System.err.println(e.getMessage());
      System.err.println("Have you tryed specifying a compatible firefox-bin with -Dwebdriver.firefox.bin=/path/to/firefox-bin");
      return;
    }
    FirefoxProfile prof = new FirefoxProfile();
    if (System.getProperty("webdriver.firefox.firebug") != null) {
      prof.addExtension(new File(System.getProperty("webdriver.firefox.firebug")));
      prof.setPreference("extensions.firebug.currentVersion", "1.8.0");
      prof.setPreference("extensions.firebug.console.enableSites", true);
      prof.setPreference("extensions.firebug.net.enableSites", true);
      prof.setPreference("extensions.firebug.script.enableSites", true);
    }

    RemoteWebDriver drv = new FirefoxDriver(bin, prof);
    try {
      GooglePlus gp = new GooglePlus(drv, args[0], args[1]);
      gp.stream(args[2]);
    } finally {
      drv.quit();
    }
  }

  public void login() throws TimeoutException {
    jQuery signin = queryUntil("a:contains(Sign in):last:visible");
    if (signin.text().contains("another")) {
      return;
    }
    signin.get().click();
    WebElement email = queryUntil("#Email").clear().get();
    email.sendKeys(username);
    WebElement pw = queryUntil("#Passwd").clear().get();
    pw.sendKeys(password);
    queryUntil("#signIn").get().click();
  }

  public void stream(String msg) throws TimeoutException {
    get("https://plus.google.com/");
    login();
    jQuery share = queryUntil(":contains(Share what's new..):last");
    share.get().click();
    getDrv().switchTo().frame(queryUntil(".editable iframe").get());
    WebElement we = getDrv().switchTo().activeElement();
    we.sendKeys(msg);
    getDrv().switchTo().defaultContent();

    jQuery to_who = queryUntil("span:contains(Add circles):last,span:contains(Add more people)");
    if (!to_who.text().contains("people")) {
      WebElement to_who_we = to_who.get();
      to_who_we.click();
      getDrv().switchTo().activeElement().sendKeys("Public\n");
      queryUntil("body").get().click();
    }
    queryUntil("td div:contains(Share):last").get().click();
  }
}
