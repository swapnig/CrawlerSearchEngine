package com.main_search_documents;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import com.build_index.BuildIndexes;
import com.crawler.WebCrawler;
import com.generic_utilities.ExtractInfo;
import com.rank_documents.DocumentPreProcessor;
import com.rank_documents.DocumentRanker;

/**
 * 
 * @author Swapnil Gupta
 *
 *         Main class that interacts as a interface to interact with the Search Engine
 *         Provides
 *         1. Web Crawler - Expects seed url and a text file name to store crawled url's
 *         2. Online Indexer - Creates indexes from urls(on the Internet) in above urls.txt
 *         3. Offline Indexer - Creates indexes from webpages stored locally on computer
 *         4. Rank Documents - Rank documents using the indexes generated from above to rank documents
 *         5. Extract Info - Extract required about any document and term
 */
public class SearchDocuments implements FileNamesInterface {

    public static String newline = "\n";
    public static String xmlFileExtension = ".xml";
    public static String textFileExtension = ".txt";
    public static DocumentRanker ranker; // Global object to avoid pre processing of documents again


    public static void main (String[] args) {

        checkContinueMenu(0, "Continue in main menu (y/n):", null); // Start and loop on main menu till user wants to
                                                                    // exit
        System.out.println(newline + "Program execution complete");
    }


    /*
     * This a generic method which repeatedly displays a specific menu based on menuId till user wants to exit
     * 	1. menuId :	menu id for menu which is intended to be repeated 
     * 		0 : default or main menu
     * 		1 : Rank documents menu
     * 		2 : Extract information menu
     * 	2. message : message that is shown to user asking whether to continue in current menu
     * 	3. ranker : optional for all other menu's except rank documents menu
     */
    public static void checkContinueMenu (int menuId, String message, DocumentRanker ranker) {

        String consent = "y";
        boolean goBack = false;

        while (!consent.equals("n")) {

            if (consent.equals("y")) {

                switch (menuId) {
                    case 1:
                        goBack = displayRankMenu(ranker); // Display ranking menu
                        break;
                    case 2:
                        goBack = displayReadMenu(); // Display read menu
                        break;
                    default:
                        goBack = displayMainMenu(); // Display main menu
                }
            }
            if (goBack == true) // If wants to go back break out of loop
                break;
            System.out.println(newline + message);
            consent = new Scanner(System.in).next(); // Check for user consent
        }
    }


    /*
     * Display main menu
     */
    public static boolean displayMainMenu () {

        System.out.println(newline + "What do you want to do");
        System.out.println("1. Crawl urls from web*");
        System.out.println("2. Build indexes from online urls");
        System.out.println("3. Build indexes from local files");
        System.out.println("4. Rank documents***");
        System.out.println("5. Get Info");
        System.out.println("6. Exit Program");
        System.out.println("Note : ");
        System.out
                .println("*Crawler crawls maximum 100 links and needs list of allowed domains. Change source file WebCrawler.java if required");
        System.out.println("**You need to build indexes to rank documents or to get info");

        String choice = new Scanner(System.in).next(); // Get user choice for main menu

        if (choice.matches("\\d+")) {

            int choiceId = Integer.parseInt(choice);
            switch (choiceId) {

                case 1: // Crawl web pages

                    WebCrawler crawler = new WebCrawler(); // Intantiate crawler object
                    crawler.setupAllowedDomains(); // Setup allowed domains for crawler
                    String seedUrl = getValidUrl(crawler, "Enter seed url"); // Get valid seed url from user

                    if (null != seedUrl) { // If valid seed url then start crawling from this seed

                        System.out.println(newline + "Please enter output file name");
                        String outputFileName = new Scanner(System.in).next(); // Get output filename

                        crawler.crawlWebPages(seedUrl, outputFileName); // Crawl webpages into output file starting from
                                                                        // seed url
                    }
                    break;

                case 2: // Build indexes from online urls

                    String textMessage = "Please enter a valid text filename with .txt extension containing urls to be indexed";
                    String urlsFileName = getValidFile(textFileExtension, textMessage); // Get valid urls text file from
                                                                                        // user

                    if (null != urlsFileName) { // If valid urls file build indexes else continue to main menu
                        System.out.println("If you want to use a stop list text file give its path below");
                        String stopListFileName = new Scanner(System.in).next(); // Get stop list path

                        new BuildIndexes().buildIndex(urlsFileName, stopListFileName, false); // Build indexes
                    }
                    break;

                case 3: // Build indexes from offline webpages

                    String corpusPath = getValidDirectory("Enter corpus path"); // Get valid corpus directory from user

                    if (null != corpusPath) { // If valid corpus path build indexes else continue to main menu
                        System.out.println("If you want to use a stop list text file give its path below");
                        String stopListFileName = new Scanner(System.in).next(); // Get stop list path

                        new BuildIndexes().buildIndex(corpusPath, stopListFileName, true); // Build indexes
                    }
                    break;

                case 4: // Rank indexed documents

                    String xmlMessage = "Please enter a valid xml filename with xml extension containing searched queries";
                    String queryXmlFileName = getValidFile(xmlFileExtension, xmlMessage); // Get valid query xml from
                                                                                          // user

                    if (null != queryXmlFileName) { // If valid xml rank documents else continue to main menu

                        System.out.println("If you want to use a stop list text file give its path below");
                        String stopList = new Scanner(System.in).next(); // Get stop list path

                        if (null == ranker) { // Ensures that ranker object is only created once
                            System.out.println(newline + "Processing documents...." + newline);
                            ranker = new DocumentRanker();
                            DocumentPreProcessor preProcess = new DocumentPreProcessor();
                            ranker.rankingPreProcess(preProcess, queryXmlFileName, stopList); // Pre process all queries
                                                                                              // and documents
                            preProcess = null; // Pre processing done make object eligible for gc
                        }

                        checkContinueMenu(1, "Continue in ranking menu (y/n):", ranker);
                    }
                    break;

                case 5: // Extract information from indexes
                    checkContinueMenu(2, "Continue in extract information (y/n):", null);
                    break;

                case 6: // User wants to exit
                    return true;

                default:
                    System.out.println("Incorrect input");
            }
        } else
            System.out.println("Incorrect input");

        return false;
    }


    /*
     * Display rank menu for documents
     * Supported ranking models:
     * 	1. Okapi TF
     * 	2. TF-IDF
     * 	3. Okapi BM-25
     * 	4. Language model with Laplace Smoothing
     * 	5. Language model with Jelinek-Mercer Smoothing
     */
    public static boolean displayRankMenu (DocumentRanker ranker) {

        System.out.println(newline + "What do you want to do");
        System.out.println("1. Okapi TF");
        System.out.println("2. TF-IDF");
        System.out.println("3. Okapi BM-25");
        System.out.println("4. Language model with Laplace Smoothing");
        System.out.println("5. Language model with Jelinek-Mercer Smoothing");
        System.out.println("6. Exit Ranking");

        String choice = new Scanner(System.in).next();
        if (choice.matches("\\d+")) {

            int scoringFunction = Integer.parseInt(choice);
            if (scoringFunction <= 5) {

                System.out.println(newline + "Please enter output file name");
                String outputFileName = new Scanner(System.in).next(); // Get output filename

                ranker.rankDocuments(scoringFunction, outputFileName); // Rank documents by given scoring function with
                                                                       // results in output file
            } else
                if (scoringFunction == 6)
                    return true; // User wants to return to main menu
                else
                    System.out.println(newline + "Incorrect input");
        } else
            System.out.println(newline + "Incorrect input");

        return false; // User wants to continue in current menu
    }


    /*
     * Call index read interface return true if want to go back else return false
     * Supported features:
     * 	1. Query for term
     * 	2. Query for document
     * 	3. Query for term in document
     */
    public static boolean displayReadMenu () {

        ExtractInfo info = new ExtractInfo();

        System.out.println(newline + "What do you want to do");
        System.out.println("1. Search term");
        System.out.println("2. Search document");
        System.out.println("3. Search term in document");
        System.out.println("4. Go back to main menu");

        String choice = new Scanner(System.in).next();

        if (choice.matches("\\d+")) {

            int choiceId = Integer.parseInt(choice);
            switch (choiceId) {

                case 1:
                    System.out.println(newline + "Enter Term");
                    info.getTerm(new Scanner(System.in).next(), termsIdFile, termInfoFile); // Get term info
                    break;

                case 2:
                    System.out.println(newline + "Enter document name");
                    info.getDoc(new Scanner(System.in).next(), docIdFile, docIndexFile); // Get document info
                    break;

                case 3:
                    Scanner input = new Scanner(System.in);
                    System.out.println(newline + "Enter Term");
                    String term = input.next();
                    System.out.println("Enter Document name");
                    String doc = input.next();

                    info.getTermInDoc(term, doc, docIdFile, termsIdFile, termIndexFile, termInfoFile); // Get term info
                                                                                                       // in document
                    break;

                case 4: // User want to return to main menu
                    return true;

                default:
                    System.out.println(newline + "Incorrect input");
            }
        } else
            System.out.println(newline + "Incorrect input");

        return false; // User wants to continue in extract info menu
    }


    /* 
     * Ensure the seed url provided is a valid url else return null
     * Expects crawler object and messgae to show to user
     * seedUrl = "http://www.ccs.neu.edu/"
     */
    public static String getValidUrl (WebCrawler crawler, String message) {

        System.out.println(newline + message);
        String seedUrl = new Scanner(System.in).next();

        while ((seedUrl != null) && (null == crawler.processUrl(seedUrl)))
            // Check for valid seed url
            seedUrl = getUserInput(message);

        return seedUrl;
    }


    /*
     * Get file with given extension else return null
     */
    public static String getValidFile (String extension, String message) {

        System.out.println(newline + message);
        String filePath = new Scanner(System.in).next();

        try {
            while ((filePath != null)
                    && (!new File(filePath).getCanonicalFile().exists() || !filePath.endsWith(extension)))
                // Check for file with appropriate extension
                filePath = getUserInput(message);
        } catch (IOException e) {
            System.err.println(filePath + " is not a valid file path");
        }
        return filePath;
    }


    /*
     * Ensure the provided corpus path is a valid directory else return null
     */
    public static String getValidDirectory (String message) {

        System.out.println(newline + message);
        String directoryPath = new Scanner(System.in).next();

        try {
            while ((directoryPath != null) && (!new File(directoryPath).getCanonicalFile().isDirectory()))
                // Check for valid directory
                directoryPath = getUserInput(message);
        } catch (IOException e) {
            System.err.println(directoryPath + " is not a valid directory path");
        }
        return directoryPath;
    }


    /*
     * Check whether user wants to give input, if yes return the input else exit
     * Expects message to be shown to user when invalid input is passed
     */
    public static String getUserInput (String message) {

        System.out.println(message + " OR enter 0 to exit");

        String userInput = new Scanner(System.in).next(); // Get user input
        if (userInput.equals("0")) // User wants to exit, return null
            return null;

        return userInput;
    }

}
