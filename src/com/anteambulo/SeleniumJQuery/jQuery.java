package com.anteambulo.SeleniumJQuery;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openqa.selenium.WebElement;

public class jQuery implements Iterable<WebElement> {
  private String selector;
  private Long length;
  private final String ref;
  private final jQueryFactory jqf;
  private boolean closed = false;
  private final jQuery parent;
  private final List<jQuery> children = new ArrayList<jQuery>();

  public static abstract class Eacher {
    public abstract void invoke(Integer index, WebElement ele);
  }

  public static abstract class Sorter {
    public abstract int invoke(WebElement a, WebElement b);
  }

  public static abstract class Mapper<T> {
    public abstract T invoke(Integer index, WebElement ele);
  }

  public static String createRef(long ref) {
    return "window.jquery_proxy_" + ref;
  }

  public String createRef() {
    return createRef(jqf.createId());
  }

  public jQuery(jQueryFactory jqf, String reference, jQuery parent) {
    this.jqf = jqf;
    this.ref = reference;
    this.parent = parent;
  }

  public jQuery(jQueryFactory jqf, long id, String query) {
    this(jqf, createRef(id), query, null);
  }

  public jQuery(jQueryFactory jqf, String reference, String query) {
    this(jqf, reference, query, null);
  }

  public jQuery(jQueryFactory jqf, String reference, String query, jQuery parent) {
    this(jqf, reference, parent);

    if (parent != null) {
      this.selector = parent.selector + query;
    } else {
      this.selector = query;
    }

    if (query != null && parent == null) {
      init();
    }
  }

  public jQuery(jQueryFactory jqf, long id, WebElement we) {
    this(jqf, createRef(id), null, null);
    init(we);
  }

  public Object jsret(String method, Object... args) {
    return js("return " + ref + method, args);
  }

  public void jsref(String method, Object... args) {
    js(ref + method, args);
  }

  public Object js(String script, Object... args) {
    check();
    return jqf.js(script, args);
  }

  public Long length() {
    return length;
  }

  public Long refreshSize() {
    return length = (Long) jsret(".length");
  }

  private void check() {
    if (closed) {
      throw new IllegalStateException("Cannot use a closed jquery object.");
    }
  }

  public jQuery init() {
    if (!closed) {
      close();
    }
    closed = false;
    jsref(" = jQuery(arguments[0]);", selector);
    refresh();
    return this;
  }

  public jQuery init(WebElement we) {
    if (!closed) {
      close();
    }
    closed = false;
    jsref("= jQuery(arguments[0]);", we);
    refresh();
    return this;
  }

  public jQuery refresh() {
    @SuppressWarnings("unchecked")
    List<Object> refresh = (List<Object>) js("return [" + ref + ".length," + ref + ".selector]");
    length = (Long) refresh.get(0);
    selector = (String) refresh.get(1);
    return this;
  }

  public jQuery refreshUntilAtLeast(int min) throws TimeoutException {
    return refreshUntilAtLeast(min, jqf.getDefaultTimeout());
  }

  public jQuery refreshUntil() throws TimeoutException {
    return refreshUntilAtLeast(1, jqf.getDefaultTimeout());
  }

  public jQuery refreshUntil(long timeout) throws TimeoutException {
    return refreshUntilAtLeast(1, timeout);
  }

  public jQuery refreshUntilAtLeast(int min, long timeout) throws TimeoutException {
    return refreshUntil(min, -1, timeout);
  }

  public jQuery refreshUntilAtMost(int max, long timeout) throws TimeoutException {
    return refreshUntil(0, max, timeout);
  }

  public jQuery refreshUntilAtMost(int max) throws TimeoutException {
    return refreshUntil(0, max, jqf.getDefaultTimeout());
  }

  public jQuery refreshUntilNone(long timeout) throws TimeoutException {
    return refreshUntil(0, 0, timeout);
  }

  public jQuery refreshUntil(int min, int max, long timeout) throws TimeoutException {
    long to = System.currentTimeMillis() + timeout;
    while (true) {
      if (length() >= min) {
        if (max == -1 || length() <= max) {
          return this;
        }
      }

      if (System.currentTimeMillis() > to) {
        throw new TimeoutException("Looking for " + selector + " with " + min + " to " + max + " results.");
      }

      try {
        Thread.sleep(200);
        refresh();
      } catch (InterruptedException e) {
        throw new TimeoutException("Looking for " + selector + " with " + min + " to " + max + " results.");
      }
    }
  }

  public Long size() {
    return length;
  }

  @SuppressWarnings("unchecked")
  public List<WebElement> toArray() {
    return (List<WebElement>) jsret(".toArray()");
  }

  public WebElement get() {
    return get(0);
  }

  public WebElement get(int index) {
    return (WebElement) jsret(".get(" + index + ")");
  }

  public jQuery pushStack(WebElement ele) {
    jsref(".pushStack(arguments[0])", ele);
    refresh();
    return this;
  }

  public jQuery each(Eacher inv) {
    for (int i = 0; i < length; i++) {
      inv.invoke(i, get(i));
    }
    return this;
  }

  public jQuery ready() {
    jsref(".ready();");
    return this;
  }

  public jQuery eq(int index) {
    return slice(index, index + 1);
  }

  public jQuery first() {
    return slice(0, 1);
  }

  public jQuery last() {
    return slice(-1);
  }

  protected jQuery subset(String method_script, Object... args) {
    jQuery subset = new jQuery(jqf, createRef(), null, this);
    js(subset.ref + "=" + ref + method_script, args);
    subset.refresh();
    children.add(subset);
    return subset;
  }

  public jQuery slice(int start, int end) {
    return subset(".slice(arguments[0],arguments[1])", start, end);
  }

  public jQuery slice(int start) {
    return subset(".slice(arguments[0])", start);
  }

  public <T> List<T> map(Mapper<T> mapper) {
    ArrayList<T> lst = new ArrayList<T>();
    for (int i = 0; i < length; i++) {
      lst.add(mapper.invoke(i, get(i)));
    }
    return lst;
  }

  public jQuery end() {
    close();
    return this.parent;
  }

  public jQuery push(WebElement ele) {
    return pushStack(ele);
  }

  public jQuery sort() {
    jsref(".sort()");
    return this;
  }

  public Object data(String key) {
    return (Object) jsret(".data(arguments[0]);", key);
  }

  public jQuery data(String key, Object value) {
    jsref(".data(arguments[0],arguments[1]);", key, value);
    return this;
  }

  public jQuery removeData(String key) {
    jsref(".removeData(arguments[0]", key);
    return this;
  }

  public jQuery dequeue() {
    jsref(".dequeue();");
    return this;
  }

  public jQuery delay(long millis) {
    jsref(".delay(arguments[0],'fx');", millis);
    return this;
  }

  public jQuery delay(long millis, String queue_name) {
    jsref(".delay(arguments[0],arguments[1]);", millis, queue_name);
    return this;
  }

  public jQuery clearQueue(String queue_name) {
    jsref(".clearQueue(arguments[0]);", queue_name);
    return this;
  }

  public jQuery clearQueue() {
    jsref(".clearQueue('fx');");
    return this;
  }

  public String attr(String key) {
    return String.valueOf(jsret(".attr(arguments[0]);", key));
  }

  public jQuery attr(String key, String value) {
    jsref(".attr(arguments[0],arguments[1]);", key, value);
    return this;
  }

  public jQuery removeAttr(String key) {
    jsref(".removeAttr(arguments[0]);", key);
    return this;
  }

  public jQuery addClass(String cls) {
    jsref(".addClass(arguments[0]);", cls);
    return this;
  }

  public jQuery removeClass(String cls) {
    jsref(".removeClass(arguments[0]);", cls);
    return this;
  }

  public jQuery toggleClass(String cls) {
    jsref(".toggleClass(arguments[0]);", cls);
    return this;
  }

  public jQuery hasClass(String cls) {
    jsref(".hasClass(arguments[0]);", cls);
    return this;
  }

  public String val() {
    return (String) jsret(".val();");
  }

  public jQuery val(String val) {
    jsref(".val(arguments[0]);", val);
    return this;
  }

  public jQuery unbind(String event) {
    jsref(".unbind(arguments[0]);", event);
    return this;
  }

  public jQuery trigger(String event) {
    jsref(".trigger(arguments[0]);", event);
    return this;
  }

  public jQuery triggerHandler(String event) {
    jsref(".triggerHandler(arguments[0]);", event);
    return this;
  }

  public jQuery toggle() {
    jsref(".toggle();");
    return this;
  }

  public jQuery toggle(boolean toggle) {
    jsref(".toggle(arguments[0]);", toggle);
    return this;
  }

  public jQuery toggle(long duration) {
    jsref(".toggle(arguments[0]);", duration);
    return this;
  }

  public jQuery toggle(long duration, String easing) {
    jsref(".toggle(arguments[0],arguments[1]);", duration, easing);
    return this;
  }

  public jQuery die() {
    jsref(".die();");
    return this;
  }

  public jQuery die(String event) {
    jsref(".die(arguments[0]);", event);
    return this;
  }

  public jQuery blur() {
    jsref(".blur();");
    return this;
  }

  public jQuery focus() {
    jsref(".focus();");
    return this;
  }

  public jQuery focusin() {
    jsref(".focusin();");
    return this;
  }

  public jQuery focusout() {
    jsref(".focusout();");
    return this;
  }

  public jQuery load() {
    jsref(".load();");
    return this;
  }

  public jQuery resize() {
    jsref(".resize();");
    return this;
  }

  public jQuery scroll() {
    jsref(".scroll();");
    return this;
  }

  public jQuery unload() {
    jsref(".unload();");
    return this;
  }

  public jQuery click() {
    jsref(".click();");
    return this;
  }

  public jQuery dblclick() {
    jsref(".dblclick();");
    return this;
  }

  public jQuery mousedown() {
    jsref(".mousedown();");
    return this;
  }

  public jQuery mouseup() {
    jsref(".mouseup();");
    return this;
  }

  public jQuery mousemove() {
    jsref(".mousemove();");
    return this;
  }

  public jQuery mouseover() {
    jsref(".mouseover();");
    return this;
  }

  public jQuery mouseout() {
    jsref(".mouseout();");
    return this;
  }

  public jQuery mouseenter() {
    jsref(".mouseenter();");
    return this;
  }

  public jQuery mouseleave() {
    jsref(".mouseleave();");
    return this;
  }

  public jQuery change() {
    jsref(".mousechange();");
    return this;
  }

  public jQuery select() {
    jsref(".select();");
    return this;
  }

  public jQuery selected() {
    return find(" :selected");
  }

  public jQuery submit() {
    jsref(".submit();");
    return this;
  }

  public jQuery keydown() {
    jsref(".keydown();");
    return this;
  }

  public jQuery keypress() {
    jsref(".keypress();");
    return this;
  }

  public jQuery keyup() {
    jsref(".keyup();");
    return this;
  }

  public jQuery error() {
    jsref(".error();");
    return this;
  }

  public jQuery find(String selector) {
    return subset(".find(arguments[0]);", selector);
  }

  public Boolean has(String selector) {
    return (Boolean) jsret(".has(arguments[0]);", selector);
  }

  public jQuery not(String selector) {
    return subset(".not(arguments[0])", selector);
  }

  public jQuery filter(String selector) {
    return subset(".not(arguments[0])", selector);
  }

  public Boolean is(jQuery jq) {
    return (Boolean) jsret(".is(arguments[0]);", jq.ref);
  }

  public Boolean is(WebElement we) {
    return (Boolean) jsret(".is(arguments[0]);", we);
  }

  public boolean is(String selector) {
    return (Boolean) jsret(".is(arguments[0]);", selector);
  }

  public jQuery closest(String selector) {
    return subset(".closest(arguments[0])", selector);
  }

  /**
   * The return value is an integer indicating the position of the first element
   * within the jQuery object relative to its sibling elements.
   * 
   * @see http://api.jquery.com/index/
   * @return
   */
  public Long index() {
    return (Long) jsret(".index();");
  }

  /**
   * Returns an integer indicating the position of the original element relative
   * to the elements matched by the selector. If the element is not found, 
   * .index() will return -1.
   * 
   * @see http://api.jquery.com/index/
   * @return
   */
  public Long index(String selector) {
    return (Long) jsret(".index(arguments[0]);", selector);
  }

  /**
   * Returns an integer indicating the position of the passed element relative 
   * to the original collection.
   * 
   * @see http://api.jquery.com/index/
   * @return
   */
  public Long index(WebElement we) {
    return (Long) jsret(".index(arguments[0]);", we);
  }

  /**
   * Adds the html to the current set of elements
   * 
   * @return
   */
  public jQuery addHtml(String html) {
    jsref(".add(arguments[0]);", html);
    return this;
  }

  /**
   * Adds the matched elements to the current set of elements
   * 
   * @return
   */
  public jQuery add(String selector) {
    jsref(".add(arguments[0]);", selector);
    return this;
  }

  /**
   * Adds the set to the current set of elements
   * 
   * @return
   */
  public jQuery add(jQuery jq) {
    jsref(".add(" + jq.ref + ");");
    return this;
  }

  public jQuery andSelf() {
    return subset(".andSelf()");
  }

  public jQuery parent() {
    return subset(".parent()");
  }

  public jQuery parent(String selector) {
    return subset(".parent(arguments[0])", selector);
  }

  public jQuery parents() {
    return subset(".parents()");
  }

  public jQuery parents(String selector) {
    return subset(".parents(arguments[0])", selector);
  }

  public jQuery parentsUntil(String selector) {
    return subset(".parentsUntil(arguments[0])", selector);
  }

  public jQuery parentsUntil(WebElement we) {
    return subset(".parentsUntil(arguments[0])", we);
  }

  public jQuery parentsUntil(WebElement we, String filter) {
    return subset(".parentsUntil(arguments[0],arguments[1])", we, filter);
  }

  public jQuery parentsUntil(String selector, String filter) {
    return subset(".parentsUntil(arguments[0],arguments[1])", selector, filter);
  }

  public jQuery next(String filter) {
    return subset(".next(arguments[0])", filter);
  }

  public jQuery next() {
    return subset(".next()");
  }

  public jQuery prev(String filter) {
    return subset(".prev(arguments[0])", filter);
  }

  public jQuery prev() {
    return subset(".prev()");
  }

  public jQuery nextAll(String filter) {
    return subset(".nextAll(arguments[0])", filter);
  }

  public jQuery nextAll() {
    return subset(".nextAll()");
  }

  public jQuery prevAll(String filter) {
    return subset(".prevAll(arguments[0])", filter);
  }

  public jQuery prevAll() {
    return subset(".prev()");
  }

  public jQuery nextUntil(String selector) {
    return subset(".nextUntil(arguments[0])", selector);
  }

  public jQuery nextUntil(String selector, String filter) {
    return subset(".nextUntil(arguments[0],arguments[1])", selector, filter);
  }

  public jQuery nextUntil(WebElement we) {
    return subset(".nextUntil(arguments[0])", we);
  }

  public jQuery nextUntil(WebElement we, String filter) {
    return subset(".nextUntil(arguments[0],arguments[1])", we, filter);
  }

  public jQuery prevUntil(String selector) {
    return subset(".prevUntil(arguments[0])", selector);
  }

  public jQuery prevUntil(String selector, String filter) {
    return subset(".prevUntil(arguments[0],arguments[1])", selector, filter);
  }

  public jQuery prevUntil(WebElement we) {
    return subset(".prevUntil(arguments[0])", we);
  }

  public jQuery prevUntil(WebElement we, String filter) {
    return subset(".prevUntil(arguments[0],arguments[1])", we, filter);
  }

  public jQuery siblings(String selector) {
    return subset(".siblings(arguments[0])", selector);
  }

  public jQuery siblings() {
    return subset(".siblings()");
  }

  public jQuery children() {
    return subset(".children()");
  }

  public jQuery children(String selector) {
    return subset(".children(arguments[0])", selector);
  }

  public jQuery contents() {
    return subset(".contents()");
  }

  public jQuery text(String text) {
    jsref(".text(arguments[0]);", text);
    return this;
  }

  public String text() {
    return (String) jsret(".text();");
  }

  public jQuery wrapAllHtml(String html) {
    jsref(".wrapAll(arguments[0])", html);
    return this;
  }

  public jQuery wrapAll(String selector) {
    jsref(".wrapAll(arguments[0])", selector);
    return this;
  }

  public jQuery wrapAll(WebElement we) {
    jsref(".wrapAll(arguments[0])", we);
    return this;
  }

  public jQuery wrapAll(jQuery jq) {
    jsref(".wrapAll(" + jq.ref + ")");
    return this;
  }

  public jQuery wrapInnerHtml(String html) {
    jsref(".wrapInner(arguments[0])", html);
    return this;
  }

  public jQuery wrapInner(String selector) {
    jsref(".wrapInner(arguments[0])", selector);
    return this;
  }

  public jQuery wrapInner(WebElement we) {
    jsref(".wrapInner(arguments[0])", we);
    return this;
  }

  public jQuery wrapInner(jQuery jq) {
    jsref(".wrapInner(" + jq.ref + ")");
    return this;
  }

  public jQuery wrapHtml(String html) {
    jsref(".wrap(arguments[0])", html);
    return this;
  }

  public jQuery wrap(String selector) {
    jsref(".wrap(arguments[0])", selector);
    return this;
  }

  public jQuery wrap(WebElement we) {
    jsref(".wrap(arguments[0])", we);
    return this;
  }

  public jQuery wrap(jQuery jq) {
    jsref(".wrap(" + jq.ref + ")");
    return this;
  }

  public jQuery unwrapHtml(String html) {
    jsref(".unwrap(arguments[0])", html);
    return this;
  }

  public jQuery unwrap(String selector) {
    jsref(".unwrap(arguments[0])", selector);
    return this;
  }

  public jQuery unwrap(WebElement we) {
    jsref(".unwrap(arguments[0])", we);
    return this;
  }

  public jQuery unwrap(jQuery jq) {
    jsref(".unwrap(" + jq.ref + ")");
    return this;
  }

  public jQuery appendHtml(String html) {
    jsref(".append(arguments[0])", html);
    return this;
  }

  public jQuery append(String selector) {
    jsref(".append(arguments[0])", selector);
    return this;
  }

  public jQuery append(WebElement we) {
    jsref(".append(arguments[0])", we);
    return this;
  }

  public jQuery append(jQuery jq) {
    jsref(".append(" + jq.ref + ")");
    return this;
  }

  public jQuery prependHtml(String html) {
    jsref(".prepend(arguments[0])", html);
    return this;
  }

  public jQuery prepend(String selector) {
    jsref(".prepend(arguments[0])", selector);
    return this;
  }

  public jQuery prepend(WebElement we) {
    jsref(".prepend(arguments[0])", we);
    return this;
  }

  public jQuery prepend(jQuery jq) {
    jsref(".prepend(" + jq.ref + ")");
    return this;
  }

  public jQuery beforeHtml(String html) {
    jsref(".before(arguments[0])", html);
    return this;
  }

  public jQuery before(String selector) {
    jsref(".before(arguments[0])", selector);
    return this;
  }

  public jQuery before(WebElement we) {
    jsref(".before(arguments[0])", we);
    return this;
  }

  public jQuery before(jQuery jq) {
    jsref(".before(" + jq.ref + ")");
    return this;
  }

  public jQuery afterHtml(String html) {
    jsref(".after(arguments[0])", html);
    return this;
  }

  public jQuery after(String selector) {
    jsref(".after(arguments[0])", selector);
    return this;
  }

  public jQuery after(WebElement we) {
    jsref(".after(arguments[0])", we);
    return this;
  }

  public jQuery after(jQuery jq) {
    jsref(".after(" + jq.ref + ")");
    return this;
  }

  public jQuery remove() {
    jsref(".remove()");
    return this;
  }

  public jQuery remove(String selector) {
    jsref(".remove(arguments[0])", selector);
    return this;
  }

  public jQuery empty() {
    jsref(".empty()");
    return this;
  }

  public jQuery clone() {
    return subset(".clone()");
  }

  public jQuery clone(boolean with_data_and_events) {
    return subset(".clone(arguments[0])", with_data_and_events);
  }

  public jQuery clone(boolean with_data_and_events, boolean deep) {
    return subset(".clone(arguments[0],arguments[1])", with_data_and_events, deep);
  }

  public String html() {
    return (String) jsret(".html();");
  }

  public jQuery html(String html) {
    jsref(".html(arguments[0]);", html);
    return this;
  }

  public jQuery replaceWithHtml(String html) {
    return subset(".replaceWith(arguments[0])", html);
  }

  public jQuery replaceWith(String selector) {
    return subset(".replaceWith(arguments[0])", selector);
  }

  public jQuery replaceWith(WebElement we) {
    return subset(".replaceWith(arguments[0])", we);
  }

  public jQuery replaceWith(jQuery jq) {
    return subset(".replaceWith(" + jq.ref + ")");
  }

  public jQuery detach() {
    jsref(".detatch()");
    return this;
  }

  public jQuery detach(String selector) {
    jsref(".detatch(arguments[0])", selector);
    return this;
  }

  public jQuery appendToHtml(String html) {
    return subset(".appendTo(arguments[0])", html);
  }

  public jQuery appendTo(String selector) {
    return subset(".appendTo(arguments[0])", selector);
  }

  public jQuery appendTo(WebElement we) {
    return subset(".appendTo(arguments[0])", we);
  }

  public jQuery appendTo(jQuery jq) {
    return subset(".appendTo(" + jq.ref + ")");
  }

  public jQuery prependToHtml(String html) {
    return subset(".prependTo(arguments[0])", html);
  }

  public jQuery prependTo(String selector) {
    return subset(".prependTo(arguments[0])", selector);
  }

  public jQuery prependTo(WebElement we) {
    return subset(".prependTo(arguments[0])", we);
  }

  public jQuery prependTo(jQuery jq) {
    return subset(".prependTo(" + jq.ref + ")");
  }

  public jQuery insertBeforeHtml(String html) {
    return subset(".insertBefore(arguments[0])", html);
  }

  public jQuery insertBefore(String selector) {
    return subset(".insertBefore(arguments[0])", selector);
  }

  public jQuery insertBefore(WebElement we) {
    return subset(".insertBefore(arguments[0])", we);
  }

  public jQuery insertBefore(jQuery jq) {
    return subset(".insertBefore(" + jq.ref + ")");
  }

  public jQuery insertAfterHtml(String html) {
    return subset(".insertAfter(arguments[0])", html);
  }

  public jQuery insertAfter(String selector) {
    return subset(".insertAfter(arguments[0])", selector);
  }

  public jQuery insertAfter(WebElement we) {
    return subset(".insertAfter(arguments[0])", we);
  }

  public jQuery insertAfter(jQuery jq) {
    return subset(".insertAfter(" + jq.ref + ")");
  }

  public jQuery replaceAll(String selector) {
    return subset(".replaceAll(arguments[0])", selector);
  }

  public String css(String key) {
    return (String) jsret(".css(arguments[0])", key);
  }

  public jQuery css(String key, String val) {
    jsref(".css(arguments[0],arguments[1])", key, val);
    return this;
  }

  public String serialize() {
    return (String) jsret(".serialize()");
  }

  public jQuery show() {
    jsref(".show()");
    return this;
  }

  public jQuery hide() {
    jsref(".hide()");
    return this;
  }

  public jQuery stop() {
    jsref(".stop()");
    return this;
  }

  public jQuery slideDown() {
    jsref(".slideDown()");
    return this;
  }

  public jQuery slideUp() {
    jsref(".slideUp()");
    return this;
  }

  public jQuery slideToggle() {
    jsref(".slideToggle()");
    return this;
  }

  public jQuery fadeIn() {
    jsref(".fadeIn()");
    return this;
  }

  public jQuery fadeOut() {
    jsref(".fadeOut()");
    return this;
  }

  public jQuery fadeToggle() {
    jsref(".fadeToggle()");
    return this;
  }

  public static class Position {
    public final Long top;
    public final Long left;

    public Position(Long top, Long left) {
      this.top = top;
      this.left = left;
    }
  }

  public Position offset() {
    @SuppressWarnings("unchecked")
    List<Long> offset = (List<Long>) js("var offset = " + ref + ".offset();" +
      "return [offset.top,offset.left];");
    return new Position(offset.get(0), offset.get(1));
  }

  public Position position() {
    @SuppressWarnings("unchecked")
    List<Long> position = (List<Long>) js("var position = " + ref + ".position();" +
      "return [position.top,position.left];");
    return new Position(position.get(0), position.get(1));
  }

  public Position offsetParent() {
    @SuppressWarnings("unchecked")
    List<Long> offset = (List<Long>) js("var offset = " + ref + ".offsetParent();" +
      "return [offset.top,offset.left];");
    return new Position(offset.get(0), offset.get(1));
  }

  public Long scrollLeft() {
    return (Long) jsret(".scrollLeft()");
  }

  public jQuery scrollLeft(int left) {
    jsref(".scrollLeft(arguments[0])", left);
    return this;
  }

  public Long scrollTop() {
    return (Long) jsret(".scrollTop()");
  }

  public jQuery scrollTop(int top) {
    jsref(".scrollTop(arguments[0])", top);
    return this;
  }

  public Long innerHeight() {
    return (Long) jsret(".innerHeight()");
  }

  public Long outerHeight() {
    return (Long) jsret(".outerHeight()");
  }

  public Long height() {
    return (Long) jsret(".height()");
  }

  public jQuery height(int height) {
    jsref(".height(arguments[0])", height);
    return this;
  }

  public Long innerWidth() {
    return (Long) jsret(".innerWidth()");
  }

  public Long outerWidth() {
    return (Long) jsret(".outerWidth()");
  }

  public Long width() {
    return (Long) jsret(".width()");
  }

  public jQuery width(int width) {
    jsref(".width(arguments[0])", width);
    return this;
  }

  public jQuery enable() {
    removeAttr("disabled");
    return this;
  }

  public jQuery disable() {
    attr("disabled", "disabled");
    return this;
  }

  public void close() {
    if (!closed) {
      js("delete " + ref);
      closed = true;
      for (jQuery child : children) {
        child.close();
      }
    }
  }

  public String toString() {
    if (selector != null) {
      return "$(" + selector + ")[" + length + "]:" + ref;
    } else {
      return "$(object)[" + length + "]:" + ref;
    }
  }

  @Override
  public Iterator<WebElement> iterator() {
    return new Iterator<WebElement>() {
      int i = 0;

      @Override
      public void remove() {
        throw new IllegalAccessError("Not implemented");
      }

      @Override
      public WebElement next() {
        return get(i++);
      }

      @Override
      public boolean hasNext() {
        return i < length;
      }
    };
  }
}
