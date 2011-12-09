package com.anteambulo.SeleniumJQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

public class jQ {

  private JavascriptExecutor js;

  public jQ() {
    js = null;
  }

  private int load_tag_index = 0;

  public void waitUntilLoaded() {
    String tag = "__load_tag_" + (load_tag_index++);
    try {
      js("jQuery(function(){" +
        "  window." + tag + "=true;" +
        "}));");
    } catch (WebDriverException e) {
      if (!include()) {
        throw e;
      }
      js("jQuery(function(){" +
        "  window." + tag + "=true;" +
        "}));");
    }
    jsWait("return window." + tag);
  }

  public jQ(JavascriptExecutor drv) {
    this.js = drv;
  }

  public void setJs(JavascriptExecutor js) {
    if (js == null) {
      throw new IllegalArgumentException("JavascriptExecutor should not be null.");
    }
    this.js = js;
  }

  public JavascriptExecutor getJs() {
    if (js == null) {
      throw new IllegalStateException("JavascriptExecutor should have been set.");
    }
    return js;
  }

  public Object js(String eval, Object... args) {
    return getJs().executeScript(eval, args);
  }

  public Object jsWait(String eval, Object... args) {
    try {
      return jsWaitSafe(eval, args);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public Object jsWait(long timeout, String eval, Object... args) {
    try {
      return jsWaitSafe(timeout, eval, args);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  private long defaultTimeout = 30000;

  public long getDefaultTimeout() {
    return defaultTimeout;
  }

  public jQ setDefaultTimeout(long defaultTimeout) {
    this.defaultTimeout = defaultTimeout;
    return this;
  }

  public Object jsWaitSafe(String eval, Object... args) throws TimeoutException {
    return jsWaitSafe(getDefaultTimeout(), eval, args);
  }

  public Object jsWaitSafe(long timeout, String eval, Object... args) throws TimeoutException {
    final long to = System.currentTimeMillis() + timeout;
    while (true) {
      Object ret = js(eval, args);
      if (ret != null) {
        if (ret instanceof Boolean) {
          if ((Boolean) ret) {
            return ret;
          }
        } else {
          return ret;
        }
      }
      if (to < System.currentTimeMillis()) {
        throw new TimeoutException("Timeout waiting for js: " + eval);
      }
    }
  }

  public static class jQElement extends jQ {
    final private WebElement we;
    final private String selector;

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof jQElement) {
        return we.equals(((jQElement) obj).we);
      }
      return super.equals(obj);
    }

    public String getSelector() {
      return selector;
    }

    public WebElement get() {
      return we;
    }
    
    public jQElement next(){
      WebElement p = (WebElement) getJs().executeScript("return jQuery(arguments[0]).next().get(0)", we);
      return new jQElement(getSelector() + ":next()", p, getJs());
    }

    public jQElement change() {
      return trigger("change");
    }

    public jQElement blur() {
      return trigger("blur");
    }

    public jQElement focus() {
      return trigger("focus");
    }

    public jQElement click() {
      if (we != null) {
        getJs().executeScript("jQuery(arguments[0]).click()", we);
        if (tagName().equalsIgnoreCase("option")) {
          jQElement parent = parent("select");
          parent.val(val()).change();
        }
      }
      return this;
    }

    public jQElement parent(String query) {
      if (we != null) {
        WebElement p = (WebElement) getJs().executeScript("return jQuery(arguments[0]).parents(arguments[1]).get(0)", we, query);
        return new jQElement(query, p, getJs());
      }
      return this;
    }

    public jQElement parent() {
      if (we != null) {
        WebElement p = (WebElement) getJs().executeScript("return jQuery(arguments[0]).parent().get(0)", we);
        return new jQElement(getSelector() + ":parent", p, getJs());
      }
      return this;
    }

    public jQElement trigger(String event) {
      if (we != null) {
        getJs().executeScript("jQuery(arguments[0]).trigger(arguments[1])", we, event);
      }
      return this;
    }

    public boolean visible() {
      return is(":visible");
    }

    public jQElement(String selector, WebElement we, JavascriptExecutor js) {
      super(js);
      this.we = we;
      this.selector = selector;
    }

    public String val() {
      if (we != null) {
        return String.valueOf(getJs().executeScript("return jQuery(arguments[0]).val()", we));
      }
      return "";
    }

    public jQElement val(String val) {
      if (we != null) {
        getJs().executeScript("jQuery(arguments[0]).val(arguments[1])", we, val);
      }
      return this;
    }

    public jQElement find(String query) {
      try {
        return findSafe(query);
      } catch (NoSuchElementException e) {
        return new jQElement(query, null, getJs());
      }
    }

    public jQElement findSafe(String query) throws NoSuchElementException {
      WebElement we;

      if (this.we != null) {
        we = (WebElement) getJs().executeScript("" +
          "var jq = jQuery(arguments[0]).find(arguments[1]);" +
          "return jq.get(0);", this.we, query);
      } else {
        we = (WebElement) getJs().executeScript("" +
          "var jq = jQuery(document).find(arguments[0]);" +
          "return jq.get(0);", query);
      }
      if (we == null) {
        throw new NoSuchElementException(query);
      }
      return new jQElement(query, we, getJs());
    }

    @SuppressWarnings("unchecked")
    public List<jQElement> findAll(String query) {
      List<jQElement> ret = new ArrayList<jQElement>();

      List<WebElement> src;
      if (this.we == null) {
        src = (List<WebElement>) getJs().executeScript("return jQuery(document).find(arguments[0]).get()", query);
      } else {
        src = (List<WebElement>) getJs().executeScript("return jQuery(arguments[0]).find(arguments[1]).get()", this.we, query);
      }
      for (WebElement we : src) {
        ret.add(new jQElement(query, we, getJs()));
      }
      return ret;
    }

    public String attr(String attr) {
      if (we == null) {
        return "";
      }
      String ret = String.valueOf(getJs().executeScript("return jQuery(arguments[0]).attr(arguments[1])", we, attr));
      if (ret == null) {
        ret = "";
      }
      return ret;
    }

    public String css(String css) {
      if (we == null) {
        return "";
      }
      return (String) getJs().executeScript("return jQuery(arguments[0]).css(arguments[1])", we, css);
    }

    public String tagName() {
      if (we == null) {
        return "";
      }
      return we.getTagName();
    }

    public String html() {
      if (we == null) {
        return "";
      }
      String ret = (String) getJs().executeScript("return jQuery(arguments[0]).html()", we);
      if (ret == null) {
        return "";
      } else {
        return ret.trim();
      }
    }

    public String text() {
      if (we == null) {
        return "";
      }
      String ret = (String) getJs().executeScript("return jQuery(arguments[0]).text()", we);
      if (ret == null) {
        return "";
      } else {
        return ret.trim();
      }

    }

    public String textVisible() {
      if (we == null) {
        return "";
      }
      return we.getText();
    }

    public jQElement type(CharSequence... keys) {
      if (we == null) {
        return this;
      }
      we.sendKeys(keys);
      return this;
    }

    public jQElement submit() {
      if (we == null) {
        return this;
      }
      getJs().executeScript("jQuery(arguments[0]).submit()", we);
      return this;
    }

    public boolean is(String query) {
      if (we == null) {
        return false;
      }
      return (Boolean) getJs().executeScript("return jQuery(arguments[0]).is(arguments[1])", we, query);
    }

    public jQElement clear() {
      return val("");
    }

    public jQElement sendKeys(String keys) {
      return type(keys);
    }

    public WebElement ele() {
      return we;
    }
  }

  protected boolean include() {
    if ((Boolean) getJs().executeScript("return typeof jQuery != typeof __undefined")) {
      return false;
    }

    long to = System.currentTimeMillis() + getDefaultTimeout();
    while ((Boolean) getJs().executeScript("return typeof jQuery == typeof __undefined")) {
      getJs().executeScript("" +
        "var src = document.createElement('script');" +
        "src.src='http://code.jquery.com/jquery-1.7.min.js';" +
        "src.type='application/javascript';" +
        "document.body.appendChild(src);");

      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      if (to < System.currentTimeMillis()) {
        throw new RuntimeException("Timeout loading jquery");
      }
    }
    return true;
  }

  public static class NoSuchElementException extends Exception {

    public NoSuchElementException(String msg) {
      super(msg);
    }

    public NoSuchElementException(Throwable e) {
      super(e);
    }

  }

  public interface Transmuter<A, B> {
    public B transmute(A from);
  }

  public <T> List<T> each(List<jQElement> query, Transmuter<jQElement, T> each) {
    List<T> ret = new ArrayList<T>();
    for (jQElement jq : query) {
      ret.add(each.transmute(jq));
    }
    return ret;
  }

  public <T> List<T> each(String query, Transmuter<jQElement, T> each) {
    List<T> ret = new ArrayList<T>();
    for (jQElement jq : findAll(query)) {
      ret.add(each.transmute(jq));
    }
    return ret;
  }

  @SuppressWarnings("unchecked")
  public List<jQElement> findAll(String query) {
    List<jQElement> ret = new ArrayList<jQElement>();
    // Tail call emulation
    while (true) {
      try {
        for (WebElement we : (List<WebElement>) getJs().executeScript("return jQuery(arguments[0]).get()", query)) {
          ret.add(new jQElement(query, we, js));
        }
      } catch (WebDriverException e) {
        if (include()) {
          // Tail call emulation
          continue;
        } else {
          throw e;
        }
      }
      return ret;
    }
  }

  public int count(String query) {
    return ((Long) getJs().executeScript("return jQuery(arguments[0]).size()", query)).intValue();
  }

  public List<jQElement> waitUntilLessThan(String query, int too_many) {
    return waitUntilLessThan(query, too_many, getDefaultTimeout());
  }

  public List<jQElement> waitUntilLessThan(String query, int too_many, long timeout) {
    try {
      return waitUntilLessThanSafe(query, too_many, timeout);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public List<jQElement> waitUntilLessThanSafe(String query, int too_many) throws TimeoutException {
    return waitUntilLessThanSafe(query, too_many, getDefaultTimeout());
  }

  public List<jQElement> waitUntilLessThanSafe(String query, int too_many, long timeout) throws TimeoutException {
    long to = System.currentTimeMillis() + timeout;
    while (true) {
      List<jQElement> ret = findAll(query);
      if (ret.size() < too_many) {
        return ret;
      }
      if (System.currentTimeMillis() > to) {
        throw new TimeoutException();
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public List<jQElement> waitUntilMoreThan(String query, int max) {
    try {
      return waitUntilMoreThanSafe(query, max);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public List<jQElement> waitUntilMoreThanSafe(String query, int max) throws TimeoutException {
    return waitUntilMoreThanSafe(query, max, getDefaultTimeout());
  }

  public List<jQElement> waitUntilMoreThanSafe(String query, int more_than, long timeout) throws TimeoutException {
    long to = System.currentTimeMillis() + timeout;
    while (true) {
      List<jQElement> ret = findAll(query);
      if (ret.size() > more_than) {
        return ret;
      }

      if (System.currentTimeMillis() > to) {
        throw new TimeoutException();
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public jQElement findSafe(String query) throws NoSuchElementException {
    jQElement ret = find(query);
    if (ret.we == null) {
      throw new NoSuchElementException(query);
    }
    return ret;
  }

  public jQElement find(String query, Object... args) {
    try {
      WebElement we = (WebElement) getJs().executeScript("return jQuery(arguments[0]).get(0)", String.format(query, args));
      return new jQElement(query, we, js);
    } catch (WebDriverException e) {
      if (!include()) {
        throw e;
      } else {
        return find(query, args);
      }
    }
  }

  public jQElement findOrWait(String query) {
    return findAllOrWait(query, 1).get(0);
  }

  public jQElement findOrWait(String query, long timeout) {
    return findAllOrWait(query, 1, timeout).get(0);
  }

  public jQElement findOrWaitSafe(String query) throws NoSuchElementException {
    try {
      return findAllOrWaitSafe(query, 1).get(0);
    } catch (TimeoutException e) {
      throw new NoSuchElementException(e);
    }
  }

  public List<jQElement> findAllOrWait(String query) {
    return findAllOrWait(query, 1);
  }

  public List<jQElement> findAllOrWait(String query, int min) {
    try {
      return findAllOrWaitSafe(query, min);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public List<jQElement> findAllOrWait(String query, int min, long timeout) {
    try {
      return findAllOrWaitSafe(query, min, timeout);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public ChangeWatch watch(String query, Object... args) {
    return new ChangeWatch(this, String.format(query, args));
  }

  public jQElement findOrWaitSafe(String query, long timeout) throws NoSuchElementException {
    try {
      return findAllOrWaitSafe(query, 1, timeout).get(0);
    } catch (TimeoutException e) {
      throw new NoSuchElementException(e);
    }
  }

  public List<jQElement> findAllOrWaitSafe(String query, int min) throws TimeoutException {
    return findAllOrWaitSafe(query, min, getDefaultTimeout());
  }

  public List<jQElement> findAllOrWaitSafe(String query, int min, long timeout) throws TimeoutException {
    long to = System.currentTimeMillis() + timeout;
    while (true) {
      List<jQElement> ret = findAll(query);
      if (ret.size() >= min) {
        return ret;
      }
      if (to < System.currentTimeMillis()) {
        throw new TimeoutException("Timeout waiting for query: " + query + " to return at least " + min + " element(s).");
      }
    }
  }

  public static class ChangeWatch {
    private List<jQElement> first;
    private List<String> html;
    private jQ jq;
    private String query;
    private static AtomicInteger count = new AtomicInteger(0);

    public ChangeWatch(jQ jq, String query) {
      this.jq = jq;
      this.query = query;
      save();

    }

    public ChangeWatch save() {
      first = jq.findAll(query);
      html = new ArrayList<String>();
      for (jQElement we : first) {
        try {
          html.add(we.html() + "-" + we.val());
        } catch (StaleElementReferenceException e) {
          html.add("uniq" + count.incrementAndGet() + "fefexxx");
        }
      }
      return this;
    }

    public ChangeWatch(String query, JavascriptExecutor drv) {
      this(new jQ(drv), query);
    }

    public List<jQElement> waitUntilChanged(int minimum) {
      return waitUntilChanged(getDefaultTimeout(), minimum);
    }

    public List<jQElement> waitUntilChanged(long timeout) {
      return waitUntilChanged(timeout, 0);
    }

    public List<jQElement> waitUntilChanged() {
      return waitUntilChanged(getDefaultTimeout(), 0);
    }

    public List<jQElement> waitUntilChanged(long timeout, int minimum) {
      try {
        return waitUntilChangedSafe(timeout, minimum);
      } catch (TimeoutException e) {
        throw new RuntimeException(e);
      }
    }

    private long defaultTimeout = 30000;

    public long getDefaultTimeout() {
      return defaultTimeout;
    }

    public ChangeWatch setDefaultTimeout(long defaultTimeout) {
      this.defaultTimeout = defaultTimeout;
      return this;
    }

    public List<jQElement> waitUntilChangedSafe() throws TimeoutException {
      return waitUntilChangedSafe(getDefaultTimeout());
    }

    public List<jQElement> waitUntilChangedSafe(long timeout) throws TimeoutException {
      return waitUntilChangedSafe(timeout, 0);
    }

    public List<jQElement> waitUntilChangedSafe(int minimum) throws TimeoutException {
      return waitUntilChangedSafe(getDefaultTimeout(), minimum);
    }

    public List<jQElement> waitUntilChangedSafe(long timeout, int minimum) throws TimeoutException {
      final long to = System.currentTimeMillis() + timeout;
      while (true) {
        List<jQElement> changed = jq.findAll(query);
        if (changed.size() >= minimum) {
          // Simple size check
          if (changed.size() != first.size()) {
            return changed;
          }

          // Detailed check
          for (int i = 0; i < first.size(); i++) {
            jQElement ch = changed.get(i);
            jQElement fs = first.get(i);

            // Sub element equality
            if (!ch.equals(fs)) {
              return changed;
            }

            // Raw text check
            String ch_text = ch.html() + "-" + ch.val();
            if (!ch_text.equals(html.get(i))) {
              return changed;
            }
          }
        }
        if (System.currentTimeMillis() > to) {
          throw new TimeoutException();
        }
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          throw new TimeoutException();
        }
      }
    }

    public String getQuery() {
      return query;
    }
  }

}
