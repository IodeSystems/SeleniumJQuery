package com.anteambulo.SeleniumJQuery;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

public class jQueryFactory {
  
  public static class Until{
    private long timeout;
    public Until(long until) {
      timeout = System.currentTimeMillis() + until;
    }
    public boolean checkWait(){
      if(check()){
        try {
          Thread.sleep(200);
          return true;
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      return false;
    }
    
    public void checkWaitSafe() throws TimeoutException{
      if(!checkWait()){
        throw new TimeoutException();
      }
    }
    
    public boolean check(){
      return System.currentTimeMillis() < timeout;
    }
  }
  
  private JavascriptExecutor js;
  private long defaultTimeout = 30000L;
  private final AtomicLong id_factory = new AtomicLong();

  public long getDefaultTimeout() {
    return defaultTimeout;
  }

  public void setDefaultTimeout(long defaultTimeout) {
    this.defaultTimeout = defaultTimeout;
  }

  public jQueryFactory(JavascriptExecutor js) {
    this.js = js;
  }
  
  public Until until(){
    return until(getDefaultTimeout());
  }
  
  public Until until(long timeout){
    return new Until(timeout);
  }

  public jQueryFactory() {
  }

  public void setJs(JavascriptExecutor js) {
    this.js = js;
  }

  public jQuery queryUntilAtLeast(String query, int min) throws TimeoutException {
    return queryUntilAtLeast(query, min, getDefaultTimeout());
  }

  public jQuery queryUntil(String query) throws TimeoutException {
    return queryUntilAtLeast(query, 1, getDefaultTimeout());
  }

  public jQuery queryUntil(String query, long timeout) throws TimeoutException {
    return queryUntilAtLeast(query, 1, timeout);
  }

  public jQuery queryUntilAtLeast(String query, int min, long timeout) throws TimeoutException {
    return queryUntil(query, min, -1, timeout);
  }

  public jQuery queryUntilAtMost(String query, int max, long timeout) throws TimeoutException {
    return queryUntil(query, 0, max, timeout);
  }

  public jQuery queryUntilAtMost(String query, int max) throws TimeoutException {
    return queryUntil(query, 0, max, getDefaultTimeout());
  }

  public jQuery queryUntilNone(String query, long timeout) throws TimeoutException {
    return queryUntil(query, 0, 0, timeout);
  }

  public jQuery queryUntilNone(String query) throws TimeoutException {
    return queryUntil(query, 0, 0, getDefaultTimeout());
  }

  public jQuery queryUntil(String query, int min, int max, long timeout) throws TimeoutException {
    long to = System.currentTimeMillis() + timeout;
    while (true) {
      jQuery q = query(query);

      if (q.length() >= min) {
        if (max == -1 || q.length() <= max) {
          return q;
        }
      }

      if (System.currentTimeMillis() > to) {
        throw new TimeoutException("Looking for " + query + " with " + min + " to " + max + " results.");
      }

      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new TimeoutException("Looking for " + query + " with " + min + " to " + max + " results.");
      }
    }
  }

  public jQuery query(String query) {
    return new jQuery(this, createId(), query);
  }

  public jQuery query(WebElement we) {
    return new jQuery(this, createId(), we);
  }

  public Object js(String script, Object... args) {
    if (js == null) {
      throw new IllegalStateException("Cannot run js without setting the js executor!");
    }
    try {
      return js.executeScript(script, args);
    } catch (WebDriverException e) {
      if (load()) {
        return js(script, args);
      }
      throw e;
    }
  }

  private boolean load() {
    if ((Boolean) js.executeScript("return typeof jQuery != typeof __undefined")) {
      return false;
    }

    long to = System.currentTimeMillis() + 30000;
    while ((Boolean) js.executeScript("return typeof jQuery == typeof __undefined")) {
      js.executeScript("" +
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

  public long createId() {
    return id_factory.incrementAndGet();
  }
}
