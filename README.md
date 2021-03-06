# WEBSCRAPER

This is a ```SPA - Single Page Application``` design. The server components of the application are developed in ```Scala``` using ```Play framework``` and ```Jsoup``` library. The UI/User Interface is a responsive design and developed using ```HTML5, CSS3 and Bootstrap``` framework. This application uses ``Jquery`` framework for making AJAX calls to the server and populating the results in the page.

The application will fetch HTML document information using ```Jsoup``` library such as:
1.  Title
2.  HTML Version
3.  Heading counts of ```h1, h2, h3, h4, h5, h6``` tags
3.  Check if Login Page is accessed
4.  Hyperlink counts
	- Internal and External ```anchor``` tags
	- Internal and External ```image``` tags
	- Internal and External ```link``` tags
	- Internal and External ```script``` tags
	- Internal and External ```mailto``` tags

## HEALTH CHECK OF LINKS

This application also performs health check of all the links in the HTML document whether it can be reachable from the application using
```Jsoup``` library. This operation is performed using ```Scala Futures``` through asynchronous calls performing many operations in parallel in an ```efficient and non-blocking``` way. A ```CountDownLatch``` is used to track the Future asynchronous operations and await for all the Future tasks to finish executing the tasks.

## JSOUP LIBRARY

- Jsoup implements the WHATWG HTML5 specification, and parses HTML to the same DOM as modern browsers do.
- Jsoup can scrape and parse HTML from a URL, file, or string
- Jsoup can find and extract data, using DOM traversal or CSS selectors
- Jsoup can manipulate the HTML elements, attributes, and text
- Jsoup can clean user-submitted content against a safe white-list, to prevent XSS attacks
- Jspup can output tidy HTML

## IDE SUPPORT

The code is written using ```IntelliJ IDEA Community Edition 2016.1.2```

## HOW TO BUILD AND RUN THE APPLICATION

This application uses SBT (SIMPLE BUILD TOOL) to build and run the application.

The application can be build as follows:

1.  Download and install [Scala](https://www.scala-lang.org/download/)
2.  Download and install [SBT](http://www.scala-sbt.org/download.html)
3.  Unzip the webscraper.zip file
4.  Navigate to project root directory i.e. webscraper
5.  Please enter ```sbt compile``` from the command line to compile the source files.
6.  Please enter ```sbt run``` from the command line to start and run the Play Server on port 9000
7.  Open a browser and enter http://localhost:9000
8.  Please enter website address in http(s) protocol

## HOW TO CLEAN THE BUILD

Pleas enter ```sbt clean``` from the command line from the root directory of the project.

