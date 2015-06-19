package com.rank_documents;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.tartarus.snowball.EnglishSnowballStemmerFactory;
import org.tartarus.snowball.util.StemmerException;

import com.generic_utilities.Utilities;
import com.main_search_documents.FileNamesInterface;

/**
 * 
 * @author Swapnil Gupta
 *         Pre process documents and then ranks them using the user selected scoring function
 *         Supported ranking models:
 *         1. Okapi TF
 *         2. TF-IDF
 *         3. Okapi BM-25
 *         4. Language model with Laplace Smoothing
 *         5. Language model with Jelinek-Mercer Smoothing
 *
 */
public class DocumentRanker implements FileNamesInterface {

    private static String seperator = "\t";
    private static String deltaSeperator = ":";

    /*********************************************************** Pre Processed data structures and variables *************************************************************/
    private static int totalDocumentCount; // Store total count of number of documnts in corpus
    private static double totalTermCount; // Store total count of terms in corpus
    private static double avgDocLength; // Store average document length in corpus
    private static double avgQueryLength; // Store average query length
    private static double vocabularySize; // Store corpus vocabulary size

    private static HashSet<String> stopWords; // Store all the stop words
    private static LinkedHashMap<String, String> queries; // Store all the queries in consideration
    private static HashMap<Integer, Integer> docLengths; // Store document length for each document
    private static HashMap<Integer, Double> documentMagnitudes; // Store document magnitude for each document
    private static HashMap<Integer, HashMap<String, Integer>> docTermCount; // Store document term-count for each term
                                                                            // in document
    /*******************************************************************************************************************************************************************/

    /*********************************************************** Data structures created for individual query ************************************************************/
    private static double queryTermsCorpusOccurences = 0; // Store the count of all query terms corpus occurrence
    private static Map<Integer, Double> scoredDocuments; // Store score for each document
    private static Map<Integer, Double> rankedDocuments; // Store documents ranked in decreasing order of score

    private static LinkedHashMap<String, Long> termOffsetInIndex; // Store term offset for each term in query
    private static LinkedHashMap<String, Integer> termFrequencyInQuery; // Store term frequency in each query
    private static LinkedHashMap<Integer, LinkedHashMap<String, Integer>> relevantDocuments; // Store a list of relevant
                                                                                             // documents
    private static LinkedHashMap<String, Integer> termOccurenceInDocuments; // Store total number of documents in which
                                                                            // the term occurs

    private static HashMap<String, Double> queryVector; // Query vector for each query
    private static HashMap<Integer, HashMap<String, Double>> documentVector; // Set of document vectors for each query


    /******************************************************************************************************************************************************************/

    /*
     * Rank documents using the given scoring function and output to given file name
     */
    public void rankDocuments (int scoringFunction, String outputFileName) {

        File outputFile = new File(outputFileName); // File containing ranked documents

        Utilities genUtility = new Utilities(); // Class providing general utilities
        ScoringFunctions scoringFn = new ScoringFunctions(); // Class implementing various scoring functions

        genUtility.initializeFile(outputFile); // Empty the output file for writing

        try {

            BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile.getAbsoluteFile(), true));// Writer
                                                                                                                 // for
                                                                                                                 // output
                                                                                                                 // file

            /****************************************************** Process each query using given scoring function ********************************************************/
            for (Map.Entry<String, String> query : queries.entrySet()) { // Parse each query one by one

                String queryText = query.getValue(); // Get query text from HashMap

                termOffsetInIndex = processQuery(queryText, genUtility, stopWords); // Get term offsets for all terms in
                                                                                    // the current query
                relevantDocuments = getRelevantDocuments(termOffsetInIndex); // Extract relevant document list for
                                                                             // current query

                switch (scoringFunction) {

                    case 1: // Okapi TF

                        termFrequencyInQuery = scoringFn.computeTermFrequencyInQuery(termOffsetInIndex); // Compute term
                                                                                                         // frequency in
                                                                                                         // query

                        queryVector = scoringFn.buildQueryVector(termFrequencyInQuery, avgQueryLength); // Build query
                                                                                                        // vector for
                                                                                                        // current query
                        documentVector = // Build Okapi TF document vector for current query
                        scoringFn.buildTFDocumentVector(relevantDocuments, docTermCount, docLengths, avgDocLength);
                        documentMagnitudes = scoringFn.getDocumentMagnitudes();
                        scoredDocuments = scoringFn.computeOkapiScore(documentVector, queryVector, documentMagnitudes); // Compute
                                                                                                                        // Okapi
                                                                                                                        // score
                                                                                                                        // for
                                                                                                                        // all
                                                                                                                        // relevant
                                                                                                                        // documents
                                                                                                                        // of
                                                                                                                        // current
                                                                                                                        // query
                        break;

                    case 2: // TF-IDF

                        termFrequencyInQuery = scoringFn.computeTermFrequencyInQuery(termOffsetInIndex); // Compute term
                                                                                                         // frequency in
                                                                                                         // query
                        queryVector = scoringFn.buildQueryVector(termFrequencyInQuery, avgQueryLength); // Build query
                                                                                                        // vector for
                                                                                                        // current query
                        LinkedHashMap<String, Double> termTfIdfScore = scoringFn.getTermTfIdfScores(
                                termOccurenceInDocuments, docLengths.size()); // Compute tf idf scores for all terms in
                                                                              // current query
                        documentVector = // Build TFIDF document vector for current query
                        scoringFn.buildTFIDFDocumentVector(relevantDocuments, termTfIdfScore, docTermCount, docLengths,
                                avgDocLength);
                        documentMagnitudes = scoringFn.getDocumentMagnitudes();
                        scoredDocuments = scoringFn.computeOkapiScore(documentVector, queryVector, documentMagnitudes); // Compute
                                                                                                                        // Okapi
                                                                                                                        // score
                                                                                                                        // for
                                                                                                                        // all
                                                                                                                        // relevant
                                                                                                                        // documents
                                                                                                                        // of
                                                                                                                        // current
                                                                                                                        // query
                        break;

                    case 3: // Okapi BM-25

                        termFrequencyInQuery = scoringFn.computeTermFrequencyInQuery(termOffsetInIndex); // Compute term
                                                                                                         // frequency in
                                                                                                         // query
                        scoredDocuments = // Compute BM25 score for all relevant documents of current query
                        scoringFn.computeBM25Score(relevantDocuments, termFrequencyInQuery, termOccurenceInDocuments,
                                docLengths, avgDocLength, totalDocumentCount);
                        break;

                    case 4: // Language model with Laplace Smoothing

                        scoredDocuments = // Compute Laplace score for all relevant documents of current query
                        scoringFn.computeLaplaceScore(relevantDocuments, termOffsetInIndex, docLengths, vocabularySize);
                        break;

                    case 5: // Language model with Jelinek-Mercer Smoothing
                        double queryJMConstant = queryTermsCorpusOccurences / totalTermCount; // Compute JM constant for
                                                                                              // current query

                        scoredDocuments = // Compute JM score for all relevant documents of current query
                        scoringFn.computeJMScore(relevantDocuments, termOffsetInIndex, docLengths, queryJMConstant);
                        break;
                }

                /***************************************** Scored documents ready rank them in descending order **************************************************************/

                rankedDocuments = genUtility.rankDocuments(scoredDocuments); // Rank scored documents for current query
                HashMap<Integer, String> docIds = genUtility.getDocNames(docIdFile); // Get all document id's
                genUtility.writeOutput(query.getKey(), docIds, rankedDocuments, docIdFile, outputWriter); // Write
                                                                                                          // ranked
                                                                                                          // documents
                                                                                                          // to output
                                                                                                          // file
                rankedDocuments = null; // Make rankedDocuments eligible for garbage collection

            }
            outputWriter.close();
            System.out.println(outputFile + " has been created");

        } catch (IOException e) {
            System.err.println("Could not create output file : " + outputFile.getAbsolutePath());
        }
    }


    /*
     * Get term offsets for all the terms in query
     */
    public static LinkedHashMap<String, Long> processQuery (String query, Utilities genUtility,
            HashSet<String> stopWords) {

        String line;
        long offset = 0; // Initialize term offset to 0

        LinkedHashMap<String, Long> termOffsetInIndex = new LinkedHashMap<String, Long>(); // Initialize structure for
                                                                                           // holding term offsets

        for (String term : query.split(" ")) {
            term = term.toLowerCase(); // Convert tokens to lower case

            if (!stopWords.contains(term)) { // Filter Stop Words
                try {
                    term = EnglishSnowballStemmerFactory.getInstance().process(term); // Create tokens using snowball
                                                                                      // stemmer
                } catch (StemmerException e) {
                    System.out.println("Stemming failed for term: " + term);
                }

                String termId = genUtility.getID(termsIdFile, term); // Get the term id corresponding to given term

                try {

                    BufferedReader reader = new BufferedReader(new FileReader(termInfoFile)); // Read termsinfo.txt
                    while ((line = reader.readLine()) != null) { // Parse one term at a time

                        String[] tokens = line.split(seperator); // Split each line based on tab character

                        if (tokens[0].equals(termId)) { // Check for query term in corpus

                            offset = Long.parseLong(tokens[1]); // Store the offset for the term in term_index.txt
                            termOffsetInIndex.put(tokens[0], offset);

                            queryTermsCorpusOccurences += Integer.parseInt(tokens[2]); // Store the count of all query
                                                                                       // terms corpus occurrence

                            break; // Break the loop if the term found
                        }
                    }
                    reader.close(); // Close term_index.txt
                } catch (IOException e) {
                    System.err.println("Could not read file : " + termsIdFile.getAbsolutePath());
                }
            }
        }
        return termOffsetInIndex;
    }


    /*
     * Get relevant documents for all the terms in the current query
     */
    public static LinkedHashMap<Integer, LinkedHashMap<String, Integer>> getRelevantDocuments (
            HashMap<String, Long> termOffset) {

        LinkedHashMap<String, Integer> termCount; // Initialize hash map to store term-frequency pair for doc
        LinkedHashMap<Integer, LinkedHashMap<String, Integer>> relevantDocuments = new LinkedHashMap<Integer, LinkedHashMap<String, Integer>>();

        try {

            for (Map.Entry<String, Long> term : termOffset.entrySet()) { // Parse each term in query one by one

                String termId = term.getKey(); // Extract term id
                long offset = term.getValue(); // Extract term offset

                RandomAccessFile indexFile = new RandomAccessFile(termIndexFile, "r"); // Randomly access terms_index
                                                                                       // file
                indexFile.seek(offset); // Traverse to the offset within the term_index file

                String line = indexFile.readLine(); // Read line at given offset
                String[] termPoisitons = line.split(seperator); // Split each line based on tab character to find all
                                                                // term-position pairs
                String[] deltaDocId; // Initialize variable for storing document Id's

                int docId = 0; // Track unique document id in which term occurs
                int previousDocId = Integer.parseInt(termPoisitons[1].split(deltaSeperator)[0]); // Track previous
                                                                                                 // document id
                int termFrequency = 0; // Track term frequency

                for (int i = 1; i < termPoisitons.length; i++) { // Start from 1 as first element is the term id

                    deltaDocId = termPoisitons[i].split(deltaSeperator); // Split term document-position pairs
                    docId += Integer.parseInt(deltaDocId[0]); // Update document id for unique documents it is non 0
                                                              // while for different
                                                              // positions within the same document it is 0 so no effect
                                                              // of summation
                    if (docId == previousDocId)
                        termFrequency++; // Update frequency as traversing same document
                    else { // New doc id found for given term

                        if (!relevantDocuments.containsKey(previousDocId)) // Document not existing in relevant
                                                                           // documents
                            termCount = new LinkedHashMap<String, Integer>();
                        else
                            // Document exist in relevant document list
                            termCount = relevantDocuments.get(previousDocId); // Extract hash map of term-frequency
                                                                              // pairs
                        termCount.put(termId, termFrequency); // Add current term-frequency pair to term-frequency pairs
                        relevantDocuments.put(previousDocId, termCount); // Add all term-frequency pairs to relevant
                                                                         // documents list

                        termFrequency = 1; // Initialize term frequency to one
                        previousDocId = docId; // Update previous document id
                    }
                }
                if (!relevantDocuments.containsKey(previousDocId)) // Add the values for last document
                    termCount = new LinkedHashMap<String, Integer>();
                else
                    // Document exist in relevant document list
                    termCount = relevantDocuments.get(previousDocId); // Extract hash map of term-frequency pairs
                termCount.put(termId, termFrequency); // Add last term-frequency pair to term-frequency pairs
                relevantDocuments.put(previousDocId, termCount); // Add all term-frequency pairs to relevant documents
                                                                 // list

                indexFile.close(); // Close term_index RandomAccess Reader
            }
        } catch (IOException e) {
            System.err.println("Could not read file : " + termIndexFile.getAbsolutePath());
        }
        return relevantDocuments; // Return relevant documents along with their term frequencies
    }


    /*
     * Pre process all the queries and documents in corpus
     */
    public void rankingPreProcess (DocumentPreProcessor preProcess, String topicsXml, String stopList) {

        stopWords = new Utilities().getFileWords(new File(stopList), null); // Load stop words

        queries = preProcess.extractQueriesXML(new File(topicsXml)); // Extract queries from queries.xml
        avgQueryLength = preProcess.getAvgQueryLength(queries, stopWords); // Get average query length in queries.xml

        docTermCount = preProcess.getDocTermCounts(docIndexFile);
        docLengths = preProcess.getDocLengths(); // Get lengths for all documents in corpus
        avgDocLength = preProcess.getAvgDocLength(); // Get average document length in corpus
        vocabularySize = preProcess.getVocabularySize(termsIdFile); // Get vocabulary size of corpus
        termOccurenceInDocuments = preProcess.getTermOccurenceInDocuments(termInfoFile); // Get total count of number of
                                                                                         // documents in which term
                                                                                         // occurs

        totalDocumentCount = preProcess.getDocumentCount(); // Get count of total number of documents in corpus
        totalTermCount = preProcess.getCorpusTermCount(); // Get total count of terms in corpus
        // preProcess.printInfo(); //Print necessary info
    }

}
