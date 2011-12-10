package com.anteambulo.SeleniumJQuery.test;

import static junit.framework.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.anteambulo.SeleniumJQuery.jQuery;
import com.anteambulo.SeleniumJQuery.jQueryFactory;

public class jQueryTest {
  private jQueryFactory jq;

  @Before
  public void init() {
    // HtmlUnitDriver is a yenta -- QUIET YENTA, QUIET!
    System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
    System.setProperty("org.apache.commons.logging.simplelog.log.com.gargoylesoftware.htmlunit","fatal");
    
    HtmlUnitDriver drv = new HtmlUnitDriver();
    drv.setJavascriptEnabled(true);
    drv.get("http://jquery.com/");
    jq = new jQueryFactory(drv);
    
  }

  @Test
  public void querying() throws Exception {
    assertEquals(jq.query("body").get().getTagName(), "body");
    assertEquals(jq.query("body").length().longValue(), 1);
    assertEquals(jq.query("asdf").length().longValue(), 0);
    assertEquals(jq.queryUntil("body").length().longValue(), 1);
    assertEquals(jq.queryUntilNone("asdf").length().longValue(), 0);
  }

  @Test
  public void subQuerying() {
    jQuery body = jq.query("body");
    jQuery checkpoint = body.find(".jq-checkpoints li:first");
    assertEquals(checkpoint.get().getTagName(), "li");
  }

  @Test
  public void dataAndAttributes() {
    jQuery logo_link = jq.query("#jq-siteLogo");
    assertEquals("http://jquery.com", logo_link.attr("href"));
    assertEquals("215", logo_link.find("img").attr("width"));
    logo_link.addClass("test");
    assertEquals(true, logo_link.is(".test"));
    logo_link.removeClass("test");
    assertEquals(false, logo_link.is(".test"));

    jQuery forum = jq.query(".jq-forum");
    assertEquals("Forum", forum.text());
    assertEquals("<A href=\"http://forum.jquery.com/\" title=\"jQuery Forum\">Forum</A>", forum.html().trim());
  }

}
