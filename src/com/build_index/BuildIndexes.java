package com.build_index;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.tartarus.snowball.EnglishSnowballStemmerFactory;
import org.tartarus.snowball.util.StemmerException;

import com.generic_utilities.Utilities;
import com.google.code.externalsorting.ExternalSort;
import com.main_search_documents.FileNamesInterface;

/**
 * 
 * @author Swapnil Gupta
 * 
 *         Builds forward and inverted indexes
 * 
 *         doc_ids.txt - Contains id-document mapping for each document indexed
 *         term_ids.txt - Contains id-term mapping for each unique term in all the documents indexed
 *         doc_index.txt - Forward index containing positions of each term occurring in a given document(id) {docId
 *         termId [list of positions.....]}
 *         sorted_doc_index.txt - Above document index sorted by term id
 *         term_index.txt - Word level inverted index(documents & positions) containing delta encoded documents(id's)
 *         and
 *         positions for each term across corpus
 *         term_info.txt - Contains information about term {termId, offset for term in inverted index, count of
 *         occurrence in entire corpus, document count containing term}
 * 
 */
public class BuildIndexes implements FileNamesInterface {

    private static final String seperator = "\t";
    private static final String newLine = "\r\n";
    private static final String pattern = "\\w+(\\.?\\w+)*";

    int docId = 0; // Counter to track current document id in corpus
    int termsId = 0; // Counter to track current term id in corpus
    HashMap<String, Integer> terms; // Store a list of all terms in corpus
    Pattern wordPattern; // Generic word pattern used to tokenize documents
    EnglishSnowballStemmerFactory stemmer;


    /**
     * Builds indexes using
     * documentSource
     * - a text file containing list of urls to be indexed
     * - a corpus path containing list of documents stored offline
     * stoplistPath
     * - a text file containing list of stop words
     * offlineMode
     * - boolean indicating whether generating indexes from online webpages or local files
     */
    public void buildIndex (String documentSource, String stopListPath, boolean offlineMode) {

        System.out.println("\nBuilding indexes....");

        initializeOutputFiles(); // Initialize output files to be initially empty

        buildForwardIndex(documentSource, stopListPath, docIdFile, termsIdFile, docIndexFile, offlineMode); // Build
                                                                                                            // forward
                                                                                                            // index for
                                                                                                            // all files
                                                                                                            // in
                                                                                                            // document
                                                                                                            // source

        try { // Using external sort to sort forward index : doc_index.txt
            DocIndexComparator docIndexComparator = new DocIndexComparator(); // Create doc_index comparator object
            List<File> fileChunk = ExternalSort.sortInBatch(docIndexFile, docIndexComparator); // Sort chunks of
                                                                                               // doc_index.txt
            ExternalSort.mergeSortedFiles(fileChunk, sortedDocIndexFile, docIndexComparator); // Merge sorted chunks of
                                                                                              // doc_index.txt
        } catch (IOException e) {
            System.err.println("Unable to external sort forward index as I/O exception occured");
        }

        buildInvertedIndex(sortedDocIndexFile, termIndexFile, termInfoFile); // Build inverted index

        System.out.println("Indexes created in " + indexFolder + " folder in current directory");

    }


    /**
     * Builds
     * forward index : doc_index.txt while tokenizing all the files in given document source while ignoring a given list
     * of stop words
     * doc_ids.txt and term_ids.txt
     * 
     * From
     * documentSource
     * - a text file containing list of urls to be indexed
     * - a corpus path containing list of documents stored offline
     * stoplistPath
     * - a text file containing list of stop words
     * offlineMode
     * - boolean indicating whether generating indexes from online webpages or local files
     */
    public void buildForwardIndex (String documentSource, String stopListSource, File docIdFile, File termsIdFile,
            File docIndexFile, boolean offlineMode) {

        Utilities utility = new Utilities(); // Create utility object
        HashSet<String> stopWords = utility.getFileWords(new File(stopListSource), null); // Load stop words

        try {

            wordPattern = Pattern.compile(pattern); // Compile pattern to tokenize words
            stemmer = EnglishSnowballStemmerFactory.getInstance(); // Create Snowball stemmer object

            // Initialize writers for output files
            BufferedWriter docIDWriter = new BufferedWriter(new FileWriter(docIdFile.getAbsoluteFile(), true));
            BufferedWriter termsIDWriter = new BufferedWriter(new FileWriter(termsIdFile.getAbsoluteFile(), true));
            BufferedWriter docIndexWriter = new BufferedWriter(new FileWriter(docIndexFile.getAbsoluteFile(), true));

            terms = new HashMap<String, Integer>(); // Store all the terms in corpus

            if (offlineMode) { // Generate indexes offline from local files

                File[] listOfFiles = utility.getFileHandlers(documentSource); // Get file handlers for all the files in
                                                                              // corpus folder
                for (File corpusFile : listOfFiles) // Process one file at a time
                {
                    if (corpusFile.isFile()) { // Check for valid file
                        String corpusFileName = corpusFile.getName(); // Get name of one file
                        processDocument(corpusFileName, documentSource, stopWords, docIDWriter, termsIDWriter,
                                docIndexWriter, offlineMode);
                    } else
                        System.out.println(corpusFile + " is invalid file"); // Invalid file found
                }
            } else { // Generate indexes online from webpage url's
                HashSet<String> urls = utility.getFileWords(new File(documentSource), " "); // Load set of urls from
                                                                                            // text file
                Iterator<String> iterator = urls.iterator();
                while (iterator.hasNext())
                    processDocument(iterator.next(), "", stopWords, docIDWriter, termsIDWriter, docIndexWriter,
                            offlineMode);
            }

            // Close output file writers
            docIDWriter.close();
            termsIDWriter.close();
            docIndexWriter.close();
        } catch (IOException io) {
            io.printStackTrace();
            System.err.println("Unable to create forward index as I/O exception occured");
        } catch (StemmerException se) {
            se.printStackTrace();
        }

    }


    /*
     * Process an individual document, extracting all the terms and associated positions within the document
     */
    public void processDocument (String fileName, String documentSource, HashSet<String> stopWords,
            BufferedWriter docIDWriter, BufferedWriter termsIDWriter, BufferedWriter docIndexWriter, boolean offlineMode)
            throws IOException, StemmerException {

        int wordPosition = 0; // Reinitialize word position start to 0 for each document

        docIDWriter.write(++docId + seperator + fileName + newLine); // Write a document name and its id to docids.txt
                                                                     // file

        ArrayList<Integer> positions; // Store all positions for given term in a document
        HashMap<Integer, ArrayList<Integer>> docTerms = new HashMap<Integer, ArrayList<Integer>>(); // Store all
                                                                                                    // term-positions
                                                                                                    // pair in a
                                                                                                    // document

        Document doc;
        if (offlineMode)
            doc = Jsoup.parse(removeFileHeader(new File(documentSource + "/" + fileName))); // Get the document with
                                                                                            // header removed using
                                                                                            // Jsoup
        else
            doc = Jsoup.connect(fileName).userAgent("Mozilla").timeout(3000).get(); // Get the document associated with
                                                                                    // given url using Jsoup

        String parsedText = doc.text(); // Extract the text from parsed Jsoup document

        Matcher matchedWords = wordPattern.matcher(parsedText); // Extract set of words matching given pattern
        while (matchedWords.find()) {

            wordPosition++; // Increment the word counter

            String matched = matchedWords.group().toLowerCase(); // Returns input subsequence matched by the previous
                                                                 // match
            if (!stopWords.contains(matched)) { // Filter Stop Words

                matched = stemmer.process(matched); // Create tokens using snowball stemmer

                if (!terms.containsKey(matched)) { // Unique term found
                    ++termsId; // Increment unique terms count
                    terms.put(matched, termsId); // Put unique terms in hash map
                    termsIDWriter.write(termsId + seperator + matched + newLine); // Write unique terms to termids.txt
                }

                int termKey = terms.get(matched); // Extract termID for given term from hash map

                if (!docTerms.containsKey(termKey)) // Unique term found
                    docTerms.put(termKey, new ArrayList<Integer>()); // Initialize position array list for unique term

                positions = docTerms.get(termKey); // Extract existing position list for current term
                positions.add(wordPosition); // Add new position to position list for given term
            }
        }

        writeDocIndex(docId, docTerms, docIndexWriter); // Write doc index for current document
        docTerms = null;
    }


    /*
     * Write forward index: doc_index one document at a time
     * 	{docId, termId, [list of term positions within document...]}
     */
    public void writeDocIndex (int docId, HashMap<Integer, ArrayList<Integer>> docTerms, BufferedWriter docIndexWriter)
            throws IOException {

        // Process termId, List<positions>
        for (Map.Entry<Integer, ArrayList<Integer>> entry : docTerms.entrySet()) { // Iterate each term in a document
                                                                                   // one by one

            docIndexWriter.write(docId + seperator + entry.getKey()); // Write document id, term name to doc_index
            for (Integer position : entry.getValue())
                // Iterate over each term positions
                docIndexWriter.write(seperator + position); // Write one position at a time to doc_index

            docIndexWriter.newLine(); // Write a new line after each term to doc_index
        }
    }


    /*
     * Function to strip of file header(here the pattern used is consecutive occurrence of 2 new lines)                                                        
     */
    public String removeFileHeader (File corpusFile) throws IOException {

        String pattern = newLine + newLine; // Define the separator pattern
        String htmlText = FileUtils.readFileToString(corpusFile); // Use apache utils to read a file to a string

        int firstHeaderIndex = htmlText.indexOf(pattern); // Find index of first header
        int headerLessTextIndex = htmlText.indexOf(pattern, firstHeaderIndex + 1); // Find start index of header less
                                                                                   // text

        return htmlText.substring(headerLessTextIndex); // Return header less text
    }


    /*
     * Process a forward index to form a word level inverted index(documents & positions) : term_index.txt and term_info.txt
     * using forward index sorted by term id
     */
    public void buildInvertedIndex (File sortedDocIndexFile, File termIndexFile, File termInfoFile) {

        try {

            RandomAccessFile indexFile = new RandomAccessFile(termIndexFile, "rw"); // Read term_index file in random
                                                                                    // read/write access mode
            BufferedReader sortedDocIndexReader = new BufferedReader(new FileReader(
                    sortedDocIndexFile.getAbsoluteFile())); // Read doc_index.txt
            BufferedWriter termInfoWriter = new BufferedWriter(new FileWriter(termInfoFile.getAbsoluteFile(), true)); // Writer
                                                                                                                      // for
                                                                                                                      // term_info.txt

            // Variables to track information to write to term_info.txt
            int docCount = 0;
            int posCount = 0;
            int previousDocId = 0;
            String previousTermId = "1";
            long offset = indexFile.getFilePointer();
            String line = "";

            indexFile.writeBytes(previousTermId + ""); // Write first term id
            while (null != (line = sortedDocIndexReader.readLine())) { // Read doc_index.txt one line at a time

                String[] tokens = line.split(seperator); // Split input file on tokens

                int currentDocId = Integer.parseInt(tokens[0]); // Extract doc id
                String currentTermId = tokens[1]; // Extract term id

                if (!currentTermId.equals(previousTermId)) { // Encountered new term

                    // Write term_info for previous term
                    termInfoWriter.write(previousTermId + seperator + offset + seperator + posCount + seperator
                            + docCount + newLine);

                    // Start processing new term
                    indexFile.writeBytes(newLine); // Start new line for new term
                    offset = indexFile.getFilePointer(); // Get current offset of term_index file for term_info
                    indexFile.writeBytes(currentTermId + ""); // Write term index to term_index.txt

                    // Reset variables for new term
                    posCount = 0;
                    docCount = 0;
                    previousDocId = 0;
                    previousTermId = currentTermId;
                }

                indexFile.write((seperator + (currentDocId - previousDocId) + ":" + tokens[2]).getBytes()); // Write
                                                                                                            // first
                                                                                                            // delta
                                                                                                            // encoded
                                                                                                            // doc:position
                                                                                                            // pair

                int docPosCount;
                int previousPosition = Integer.parseInt(tokens[2]);
                for (docPosCount = 3; docPosCount < tokens.length; docPosCount++) {
                    int currentPosition = Integer.parseInt(tokens[docPosCount]);
                    indexFile.writeBytes(seperator + 0 + ":" + (currentPosition - previousPosition)); // Write delta
                                                                                                      // position to
                                                                                                      // term_index.txt
                    previousPosition = currentPosition; // Update previous position
                }

                docCount++; // Update document count for current term
                posCount += docPosCount - 2; // Add current document occurrence to total term occurrence
                previousDocId = currentDocId; // Update previous doc id to refer to current doc id
            }

            // Write term_info for last term
            termInfoWriter.write(previousTermId + seperator + offset + seperator + posCount + seperator + docCount
                    + newLine);

            // Close file readers/writers
            indexFile.close();
            termInfoWriter.close();
            sortedDocIndexReader.close();

        } catch (IOException e) {
            System.err.println("Unable to create inverted index as I/O exception occured");
        }
    }


    /*
     * Initialize the output files
     * 	- Create folder containing all the indexes
     * 	- Initialize all output files to be empty at time of building indexes
     */
    public static void initializeOutputFiles () {

        Utilities utility = new Utilities(); // Create utility object

        new File(indexFolder).mkdirs(); // Create folder to store all indexes
        utility.initializeFile(docIdFile); // Initialize docids.txt
        utility.initializeFile(docIndexFile); // Initialize doc_index.txt
        utility.initializeFile(termsIdFile); // Initialize termids.txt
        utility.initializeFile(termIndexFile); // Initialize term_index.txt
        utility.initializeFile(termInfoFile); // Initialize term info file
    }

}
