package com.anteambulo.SeleniumJQuery.example;

import java.util.concurrent.TimeoutException;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.anteambulo.SeleniumJQuery.jQueryFactory;
import com.gargoylesoftware.htmlunit.BrowserVersion;

public class Example extends jQueryFactory {
  public static void main(String[] args) throws TimeoutException {
    // HtmlUnitDriver is loud...
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
    System.setProperty("org.apache.commons.logging.simplelog.log.com.gargoylesoftware.htmlunit", "fatal");
    HtmlUnitDriver drv = new HtmlUnitDriver(BrowserVersion.FIREFOX_3_6);
    drv.setJavascriptEnabled(true);
    try {
      Example jq = new Example();
      jq.setJs(drv);

      drv.get("http://google.com");
      jq.query("[name=q]").val("SeleniumJQuery").parents("form:first").submit();

      String results = jq.queryUntil("#resultStats:contains(results)").text();
      System.out.println(results.split(" ")[1] + " results found!");
    } finally {
      drv.close();
    }
  }
}
