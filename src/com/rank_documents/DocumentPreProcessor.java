package com.rank_documents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Swapnil Gupta
 *         Pre processing logic for documents
 *
 */
public class DocumentPreProcessor {

    String line;
    String seperator = "\t";
    double avgDocLength, avgQueryLength;
    int vocabularySize, documentCount, corpusTermsCount, queryCount, queryTermsCount;
    HashMap<Integer, Integer> docLengths;


    /*
     * Extract queries from given XML file
     */
    public LinkedHashMap<String, String> extractQueriesXML (File topicsXML) {

        LinkedHashMap<String, String> queries = new LinkedHashMap<String, String>();
        try {

            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder(); // Make
                                                                                                  // DocumentBuilder
                                                                                                  // object for the
                                                                                                  // document
            Document doc = dBuilder.parse(topicsXML); // Get the parsed xml document
            NodeList topics = doc.getElementsByTagName("topic"); // Get list of topic nodes in xml document

            for (int count = 0; count < topics.getLength(); count++) { // Parse each xml node one at a time

                Node topicNode = topics.item(count); // Get the current topic node

                if (topicNode.getNodeType() == Node.ELEMENT_NODE) { // Check whether the current xml node is an element
                    Element topic = (Element) topicNode; // type cast current node document into element
                    String queryId = topic.getAttribute("number"); // Extract query id
                    String queryText = topic.getElementsByTagName("query").item(0).getTextContent();// Extract query
                                                                                                    // text
                    queries.put(queryId, queryText); // Put queryId:queryText pair into Hash Map
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return queries; // Return Queries HashMap
    }


    /*
     * Return average query length after removing stop words
     */
    public double getAvgQueryLength (HashMap<String, String> queries, HashSet<String> stopWords) {

        int termCount = 0; // Initialize term count to 0
        String queryTokens[];

        for (Map.Entry<String, String> query : queries.entrySet()) { // Process each query one by one
            queryTokens = query.getValue().split(" "); // Extract terms from each query
            for (String token : queryTokens) { // Process each query term one at a time
                if (!stopWords.contains(token)) // Check for query term for being a stop word
                    termCount++; // If query term not a stop word increment count by 1
            }
        }

        queryCount = queries.size(); // Store count of total number of queries into global variable
        queryTermsCount = termCount; // Store count of total number of terms in all queries into global variable
        avgQueryLength = (double) queryTermsCount / queryCount; // Compute average query length

        return avgQueryLength; // Return average query length
    }


    /*
     * Get average document length
     */
    public double getAvgDocLength () {

        return avgDocLength;
    }


    /*
     * Get total count of terms in corpus
     */
    public double getCorpusTermCount () {

        return corpusTermsCount;
    }


    /*
     * Get vocabulary size for the corpus
     */
    public int getVocabularySize (File termsIdFile) {

        vocabularySize = 0; // Initialize vocabulary size to zero

        try {
            BufferedReader reader = new BufferedReader(new FileReader(termsIdFile)); // Initialize termids.txt reader

            while ((line = reader.readLine()) != null)
                vocabularySize++; // Increment vocabulary size by one for each line

            reader.close(); // Close termids.txt reader
        } catch (IOException e) {
            System.err.println("Could not read file" + termsIdFile.getAbsolutePath());
        }

        return vocabularySize; // Return vocabulary size of corpus
    }


    /*
     * Return count of total number of documents in corpus
     */
    public int getDocumentCount () {

        return documentCount;
    }


    /*
     * Traverse doc_index.txt to compute various info
     * Hash Map containing word count of each document, average document length, total number of documents in corpus, total number of words in corpus
     */
    public HashMap<Integer, HashMap<String, Integer>> getDocTermCounts (File docIndexFile) {

        HashMap<Integer, HashMap<String, Integer>> docTermCount = new HashMap<Integer, HashMap<String, Integer>>();
        try {

            BufferedReader reader = new BufferedReader(new FileReader(docIndexFile)); // Initialize doc_index.txt reader
            HashMap<String, Integer> termCount; // Initialize hash map to store document lengths
            docLengths = new HashMap<Integer, Integer>(); // Initialize hash map to store document lengths

            int docCount = 1; // Count total number of documents in corpus
            int termFrequency = 0; // Count number of positions in a line
            int corpusTermCount = 0; // Count corpus length(total terms in corpus)
            int documentTermsCount = 0; // Count document length(total terms in document)

            while ((line = reader.readLine()) != null) { // Parse one document at a time to find its length

                String[] temp = line.split(seperator); // Split each line based on tab character
                termFrequency = temp.length - 2; // -2 as first element is docId and second is termId

                if (Integer.parseInt(temp[0]) == docCount) { // Parsing term-positions for same document
                    if (!docTermCount.containsKey(docCount)) // Document not existing in relevant documents
                        termCount = new HashMap<String, Integer>();
                    else
                        // Document exist in relevant document list
                        termCount = docTermCount.get(docCount); // Extract hash map of term-frequency pairs
                    termCount.put(temp[1], termFrequency); // Add current term-frequency pair to term-frequency pairs
                    docTermCount.put(docCount, termCount); // Add all term-frequency pairs to relevant documents list
                    documentTermsCount += termFrequency; // Update document total term count
                } else { // Encountered new document
                    corpusTermCount += documentTermsCount; // Update corpus term count
                    docLengths.put(docCount, documentTermsCount); // Add docId and its length to docLengths HashMap
                    docCount++; // Update document count
                    documentTermsCount = termFrequency; // Reset document term count to the terms encountered
                } // in first line of the current document
            }
            corpusTermCount += documentTermsCount; // Add term count for the last document to corpus
            docLengths.put(docCount, documentTermsCount); // Add term count of last document to hash map

            reader.close(); // Close doc_index.txt reader

            avgDocLength = (double) corpusTermCount / docCount; // Set average document length of corpus in global
                                                                // variable
            documentCount = docCount; // Set total document count in corpus into global variable
            corpusTermsCount = corpusTermCount; // Set total corpus term count into global variable

        }

        catch (IOException e) {
            System.err.println("Could not read file" + docIndexFile.getAbsolutePath());
        }

        return docTermCount; // Return HashMap containing document id's and their length

    }


    /*
     * Return document lengths for all the document in corpus
     */
    public HashMap<Integer, Integer> getDocLengths () {

        return docLengths;
    }


    /*
     * Extract total count of documents in which each term occurs
     */
    public LinkedHashMap<String, Integer> getTermOccurenceInDocuments (File termInfoFile) {

        LinkedHashMap<String, Integer> termOccurenceInDocuments = new LinkedHashMap<String, Integer>();// Store total
                                                                                                       // number of
                                                                                                       // documents in
                                                                                                       // which the term
                                                                                                       // occurs

        try {
            BufferedReader reader = new BufferedReader(new FileReader(termInfoFile)); // Read termsinfo.txt

            while ((line = reader.readLine()) != null) { // Parse one term at a time

                String[] tokens = line.split(seperator); // Split each line based on tab character
                String termId = tokens[0];
                int termDocCount = Integer.parseInt(tokens[3]); // Store the count of term's total document occurrences
                termOccurenceInDocuments.put(termId, termDocCount);
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Could not read file" + termInfoFile.getAbsolutePath());
        }
        return termOccurenceInDocuments;
    }


    /*
     * Print the required info
     */
    public void printInfo () {

        System.out.println("Total terms in queries : " + queryTermsCount);
        System.out.println("Query count in queries : " + queryCount);
        System.out.println("Average query length in queries : " + avgQueryLength);
        System.out.println();

        System.out.println("Unique terms in corpus (Vocabulary size): " + vocabularySize);
        System.out.println("Total terms in corpus : " + corpusTermsCount);
        System.out.println("Document count in corpus : " + documentCount);
        System.out.println("Average document length in corpus : " + avgDocLength + "\n");
    }

}
