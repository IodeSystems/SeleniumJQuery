What
==================
This binds jQuery with Selenium to give you access to a large, useful portion of the jQuery API while coding in Java.

Why
==================
Because the Selenium js/dom API sucks. No access to invisible items, no pulling data/text from arbitrary places, and a crappy selector engine.

How
==================
SeleniumJQuery packages it's own version of jQuery and injects them into the page. During usage, it tracks the jQuery objects in a page cache with a generated id (incrementing long).

When the page is reloaded, those query objects are lost, but can generally be rebuilt with the initial query and subsequent modifications.

When the object is GC'd, then it's reference in the page just hangs out -- But I figure that pages will be closed/reloaded often so this should not be an issue.

Install
==================
	$ git clone git@github.com:Nthalk/SeleniumJQuery.git # Checkout project
	$ cd SeleniumJQuery                                  # Step into dir
	$ ant test                                           # Build/test
	                                                     # Run example
	$ java -Dwebdriver.firefox.bin=/path/to/compatible/firefox-bin -jar dist/SeleniumJQuery.jar
	
Using as a Library
==================
After installing, you can reference it as an ivy library as it will be in your
ivy cache. It does not currently have a home on any maven/ivy repos.

For the current revision, your ivy.xml should have a dependency of:
	&lt;dependency org="com.anteambulo" name="SeleniumJQuery" rev="2.16.1"/&gt;
	
Or you could just build it and drop the dist dependency jars and the 
SeleniumJQuery jar in your classpath.

Requirements
==================
The only requirement is ant, as everything else should be pulled in by ant.

Example
==================
Please see: https://github.com/Nthalk/SeleniumJQuery/blob/master/example/com/anteambulo/SeleniumJQuery/example/Example.java
for a sweet example!

Development & Typical Usage
==================
Typically, you really need to see what is going on to develop a good scraper.
My setup is usually with Firefox 5.0, and I inject firebug, and some firebug properties into my drivers while I develop them. Afterwards, I use HtmlUnitDriver for production (and I silence it's excessively verbose output).
