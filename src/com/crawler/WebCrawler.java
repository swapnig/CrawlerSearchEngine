package com.crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.generic_utilities.Utilities;

import edu.uci.ics.crawler4j.url.URLCanonicalizer;

/**
 * 
 * @author Swapnil Gupta
 * 
 *         Crawler to extract links(limited to 100) from webpages starting from initial seed
 *
 */
public class WebCrawler {

    /*********************************************************************** Counters and variables *********************************************************************/
    private static final int visitedUrlsLimit = 100;
    private static final int extractedUrlsLimit = 120;

    private static int extractedUrlsCount = 0;
    private static final String urlSeperator = " ";
    private static final String htmlContentType = "text/html";

    /******************************************************************************************************************************************************************/

    /************************************************************************** Data structures *************************************************************************/
    private static HashSet<String> allowedDomains = new HashSet<String>(); // Store set of all the allowed domains
    private static HashSet<String> uniqueHosts = new HashSet<String>(); // Store set of all the unique domains helpful
                                                                        // for tracking robots.txt
    private static HashSet<String> restrictedUrls = new HashSet<String>(); // Store set of restricted urls extracted
                                                                           // from robots.txt's
    private static HashSet<String> visitedUrls = new HashSet<String>(); // Store set of visited urls increases with each
                                                                        // visited url

    /**
     * Efficient url_queue implementation : 2 separate hash set one for each BFS level one current and another next
     * level.
     * When current frontier becomes empty future_frontier becomes current and iteration continues
     * When both current and future frontier become empty execution stops
     **/
    private static HashSet<String> currentFrontier = new HashSet<String>(); // Store list of extracted urls at current
                                                                            // level of BFS iteration
    private static HashSet<String> futureFrontier = new HashSet<String>(); // Store list of extracted urls at next level
                                                                           // of BFS iteration


    /******************************************************************************************************************************************************************/

    /*
     * Crawl web pages from the given initial seed url into the output file
     */
    public void crawlWebPages (String seedUrl, String outputFile) {

        final File file = new File(outputFile); // Output file containing set of links
        new Utilities().initializeFile(file); // Initializing the file for storing the links to be initially empty

        currentFrontier.add(seedUrl); // Put the initial seed in current frontier data structure

        BufferedWriter urlWriter;
        try {

            urlWriter = new BufferedWriter(new FileWriter(file.getAbsoluteFile(), true));

            Iterator<String> iterator = currentFrontier.iterator(); // Continue till there are no more url's to parse or
                                                                    // max limit reached
            while (visitedUrls.size() <= visitedUrlsLimit && (iterator.hasNext() || !futureFrontier.isEmpty())) {

                if (!iterator.hasNext()) { // Check for current frontier being empty
                    currentFrontier = futureFrontier; // Make future frontier the current frontier
                    iterator = currentFrontier.iterator(); // Get iterator for the new current frontier
                    futureFrontier = new HashSet<String>(); // Reinitialize future frontier
                }

                String pageUrl = iterator.next(); // Get next element from current frontier

                visitedUrls.add(pageUrl); // Add visited url to visited urls set
                urlWriter.write(pageUrl);

                if (extractedUrlsCount <= extractedUrlsLimit) // Limit extracted urls early on
                    extractLinks(pageUrl, urlWriter); // Extract links from url

                iterator.remove(); // Remove visited url from extracted urls set

                urlWriter.newLine();
            }

            urlWriter.close();

        } catch (IOException e) {
            System.err.println("IO exception occurent while writing: " + file.getAbsolutePath());
        }
    }


    /*
     * Process the web page at given url and extract all the links on the page
     * Write the text file {current url [set of unique urls in this page.....]}
     */
    public void extractLinks (String url, BufferedWriter urlWriter) throws IOException {

        Document doc = Jsoup.connect(url).userAgent("Mozilla").timeout(3000).get(); // Get the document associated with
                                                                                    // given url

        Elements links = doc.select("a[href]"); // Extract all the links in this page
        for (Element link : links) {

            if (extractedUrlsCount <= extractedUrlsLimit) // Limit extracted urls
            {
                String cannonicalUrl = processUrl(link.attr("abs:href")); // Process each extracted url
                if (cannonicalUrl != null && !visitedUrls.contains(cannonicalUrl)
                        && !currentFrontier.contains(cannonicalUrl) && !futureFrontier.contains(cannonicalUrl)) {
                    extractedUrlsCount++;
                    futureFrontier.add(cannonicalUrl);
                    urlWriter.write(urlSeperator + cannonicalUrl);
                }
            } else
                break;
        }
    }


    /*
     * Process each url, check to be in allowed domain, not in restricted domain and cannonize url
     */
    public String processUrl (String pageUrl) {

        try {
            URL url = new URL(pageUrl);

            String host = url.getHost();

            if (checkDomain(host, allowedDomains)) { // Check whether host is in allowed domain or its sub domain

                if (!uniqueHosts.contains(host)) { // New host found check for robots.txt
                    uniqueHosts.add(host);
                    parseRobotsTxt(url.getProtocol(), host);
                }

                if (!checkDomain(pageUrl, restrictedUrls) && isHtmlDoc(pageUrl)) // Check url to be not restricted and a
                                                                                 // valid html
                    return URLCanonicalizer.getCanonicalURL(pageUrl); // Get canonical url using library
            }

        } catch (MalformedURLException ex) {
            return null;
        }
        return null;
    }


    /*
     * Parse the robots.txt for the given domain if it exists, not very robust
     */
    public void parseRobotsTxt (String protocol, String host) {

        try {

            URL robotURL = new URL(protocol, host, "robots.txt");
            HttpURLConnection connection = (HttpURLConnection) robotURL.openConnection();
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64) Chrome/23.0.1271.95 Safari/537.11");

            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) { // Verify existence of robots.txt for given domain

                Scanner robotsScanner = new Scanner(connection.getInputStream()); // Get robots.txt input stream
                while (robotsScanner.hasNextLine()) {

                    String[] tokens = robotsScanner.nextLine().split(" ");
                    if (tokens[0].contains("Disallow:")) {
                        String restrictedUrl = host + tokens[1].toLowerCase();

                        if (!restrictedUrls.contains(restrictedUrl)) // Add unique restricted url to restrictedUrls
                            restrictedUrls.add(restrictedUrl);
                    }
                }

                robotsScanner.close();
            }
        } catch (IOException e) {
            System.err.println("IO exception for host: " + host);
        }
    }


    /*
     * Request http header to ensure that the url refers to a valid html page
     */
    public boolean isHtmlDoc (String url) {

        try {
            // Thread.sleep(5000);
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64) Chrome/23.0.1271.95 Safari/537.11");
            con.setRequestMethod("HEAD");
            String contentType = con.getContentType();

            return ((con.getResponseCode() == HttpURLConnection.HTTP_OK) && contentType.contains(htmlContentType));

        } catch (Exception e) {
            System.err.println("Invalid url: " + url);
            return false;
        }
    }


    /*
     * Check for a string containing any of the elements in a hash set at any position
     */
    public boolean checkDomain (String url, HashSet<String> domainSet) {

        for (String allowedDomain : domainSet) {
            if (url.contains(allowedDomain)) // Match url host with each element in domain set
                return true;
        }
        return false;
    }


    /*
     * Provide allowed domains to be parsed 
     */
    public void setupAllowedDomains () {

        allowedDomains.add("northeastern.edu");
        allowedDomains.add("neu.edu");

    }

}
