package com.anteambulo.SeleniumJQuery.example;

import java.util.concurrent.TimeoutException;

import org.openqa.selenium.firefox.FirefoxDriver;

import com.anteambulo.SeleniumJQuery.jQueryFactory;

public class Example extends jQueryFactory {
  public static void main(String[] args) throws TimeoutException {
    FirefoxDriver drv = new FirefoxDriver();
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
