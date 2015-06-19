package com.generic_utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * @author Swapnil Gupta
 *         Implements certain utility methods used by search engine
 * 
 */
public class Utilities {

    String wordSeparator = "\t";
    String newLine = "\n";


    /*
     * Extract set of words(first word in each line) from given file
     * separator is used to split in case line has multiple words
     */
    public HashSet<String> getFileWords (File stopListFile, String separator) {

        HashSet<String> tokens = new HashSet<String>(); // Hash Set for storing tokens in file

        try { // Read input file line by line
            Scanner inputFile = new Scanner(stopListFile);
            if (separator != null) { // If separator exist split to get first word
                while (inputFile.hasNextLine())
                    tokens.add(inputFile.nextLine().split(separator)[0]); // Add word to hash set
            } else {
                while (inputFile.hasNextLine())
                    tokens.add(inputFile.nextLine()); // Add word to hash set
            }
            inputFile.close();

        } catch (FileNotFoundException e) {
            System.out.println("Stop list file not found at : " + stopListFile.getAbsolutePath());
        }

        return tokens; // Return hash set containing tokens
    }


    /*
     * Create the output file if it does not exist
     * If it exists initialize it to be empty
     */
    public void initializeFile (File outputFile) {

        try {

            if (!outputFile.exists()) // Creating the file if it does not exist
                outputFile.createNewFile();

            new FileOutputStream(outputFile, false).close(); // Emptying previous data in file
        } catch (FileNotFoundException e) {
            System.err.println(outputFile.getAbsolutePath() + " not found");
        } catch (IOException e) {
            System.err.println("Could not initialize file : " + outputFile.getAbsolutePath());
        }
    }


    /*
     * Extract all the filenames in given folder
     */
    public File[] getFileHandlers (String folderPath) {

        File folder = new File(folderPath); // Get list of files from given folder path
        File[] listOfFiles = folder.listFiles();

        if (null == listOfFiles) // Check for directory empty or non existent
            System.out.println("No files found at : " + folderPath);

        return listOfFiles; // Return all filenames in folder as a array
    }


    /*
     * Write the ranked documents for individual query to output file
     */
    public void writeOutput (String queryNumber, HashMap<Integer, String> docIds, Map<Integer, Double> rankedDocuments,
            File docIDFile, BufferedWriter outputWriter) {

        int rank = 1;

        for (Entry<Integer, Double> entry : rankedDocuments.entrySet()) { // Parse each element one by one

            double score = entry.getValue(); // Get the document score
            int docID = entry.getKey(); // Get the document id
            String documentName = docIds.get(docID); // Get document name using document id

            try {
                outputWriter.write(queryNumber + " 0 " + documentName + " " + rank++ + " " + score + " run1" + newLine);
            } catch (IOException e) {
                System.err.println("Could not write to output file");
            }
        }
    }


    /*
     * Extract document id's and name from text file into a hash map                                                                   
     */
    public HashMap<Integer, String> getDocNames (File docIDFile) {

        String line;
        HashMap<Integer, String> docIds = new HashMap<Integer, String>(); // Initialize doc id's and document name hash
                                                                          // map

        try {
            BufferedReader reader = new BufferedReader(new FileReader(docIDFile)); // Initialize doc_index.txt reader

            while ((line = reader.readLine()) != null) { // Iterate through docids.txt to get document id
                String[] tokens = line.split(wordSeparator); // Extract tokens
                docIds.put(Integer.parseInt(tokens[0]), tokens[1]); // Put the document id and document name in HashMap
            }

            reader.close(); // Close docids.txt reader
        } catch (IOException e) {
            System.err.println("Could not read file" + docIDFile.getAbsolutePath());
        }
        return docIds; // Return document id-name hash map
    }


    /*
     * Return id for the specified parameterName from given text file                                                          
     */
    public String getID (File readFile, String parameterName) {

        String line;

        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(readFile)); // Read termids.txt
            while ((line = fileReader.readLine()) != null) { // Parse one document at a time to find its length

                String[] temp = line.split(wordSeparator);
                if (temp[1].equals(parameterName)) { // Check for text file containing the parameter
                    fileReader.close(); // CLose file before returning control
                    return temp[0]; // Return the id for matched parameter in text file
                }
            }
            fileReader.close();
        } catch (IOException e) {
            System.out.println("Could not read the file : " + readFile.getAbsolutePath());
        }

        System.out.println(parameterName + " is not present in corpus"); // Whole document traversed parameter not found
        return null;
    }


    /*
     * Generic function to print a map                                                                                                 
     */
    public void printMap (Map<?, ?> map) {

        for (Map.Entry<?, ?> entry : map.entrySet()) { // Traverse the map
            System.out.println(entry.getKey() + " " + entry.getValue()); // Print Map elements one by one
        }
    }


    /*
     * Sort the documents in descending order of ranking
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<Integer, Double> rankDocuments (Map<Integer, Double> documentScore) {

        List list = new LinkedList(documentScore.entrySet());

        Collections.sort(list, new Comparator() {

            @Override
            public int compare (Object o1, Object o2) {

                return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
            }
        });

        // Put sorted list into map again
        // LinkedHashMap make sure order in which keys were inserted
        Map sortedMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

}
