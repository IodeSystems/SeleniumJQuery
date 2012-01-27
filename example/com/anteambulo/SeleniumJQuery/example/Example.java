package com.anteambulo.SeleniumJQuery.example;

import java.util.concurrent.TimeoutException;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import com.anteambulo.SeleniumJQuery.jQueryFactory;

public class Example extends jQueryFactory {
  public static void main(String[] args) throws TimeoutException {
    
    FirefoxBinary bin;
    try {
      bin = new FirefoxBinary();
    } catch (WebDriverException e) {
      System.err.println(e.getMessage());
      System.err.println("Have you tryed specifying a compatible firefox-bin with -Dwebdriver.firefox.bin=/path/to/firefox-bin");
      return;
    }
    
    FirefoxDriver drv = new FirefoxDriver(bin,new FirefoxProfile());
    
    try {
      Example jq = new Example();
      jq.setJs(drv);

      drv.get("http://google.com");
      jq.query("[name=q]").val("SeleniumJQuery").parents("form:first").submit();

      String results = jq.queryUntil("#resultStats:contains(results)").text();
      System.out.println(results.split(" ")[1] + " results found!");
    } finally {
      drv.quit();
    }
  }
}
