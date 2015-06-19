package com.generic_utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.tartarus.snowball.EnglishSnowballStemmerFactory;
import org.tartarus.snowball.util.StemmerException;

/**
 * 
 * @author Swapnil Gupta
 *         Interface to extract information for any term or document
 *
 */
public class ExtractInfo {

    private final String seperator = "\t";
    private final String newLine = "\n";


    /*
     * Function returning metadata for given document
     */
    public void getDoc (String filename, File docIdFile, File docIndexFile) {

        String docId = new Utilities().getID(docIdFile, filename); // Get the document ID corresponding to document name

        if (null != docId) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(docIndexFile));// Read doc_index.txt

                // Define variables
                String line;
                int termsCount = 0;
                int distinctTermsCount = 0;

                // Iterate through doc_index.txt line by line to find metadata for given doc id
                while ((line = reader.readLine()) != null) {
                    String[] temp = line.split(seperator);

                    if (temp[0].equals(docId)) {
                        distinctTermsCount++; // One distinct term per line increment distinct terms count
                        termsCount += temp.length - 2; // Increment total terms count
                    }
                }

                // Print metadata to the screen
                System.out.println(newLine + "Listing for document: " + filename);
                System.out.println("DOCID: " + docId);
                System.out.println("Distinct terms: " + distinctTermsCount);
                System.out.println("Total terms: " + termsCount);

                reader.close(); // Close doc_index.txt reader
            } catch (IOException e) {
                System.err.println("Could not read file" + docIndexFile.getAbsolutePath());
            }
        }

    }


    /*
     * Function returning metadata for given term
     */
    public void getTerm (String term, File termsIdFile, File termInfoFile) {

        String stemmed = term; // Apply snowball stemmer for stemming terms
        try {
            stemmed = EnglishSnowballStemmerFactory.getInstance().process(term);
        } catch (StemmerException e) {
            System.out.println("Stemming failed for term: " + term);
        }

        String termId = new Utilities().getID(termsIdFile, stemmed); // Get the term id corresponding to given term

        if (null != termId) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(termInfoFile));// Read terms_info.txt

                String line;
                // Iterate through terms_info.txt to find metadata for given term id
                while ((line = reader.readLine()) != null) {

                    String[] temp = line.split(seperator);
                    if (temp[0].equals(termId)) {

                        // Print metadata on output stream
                        System.out.println(newLine + "Listing for term: " + stemmed);
                        System.out.println("TERMID: " + termId);
                        System.out.println("Number of documents containing term: " + temp[3]);
                        System.out.println("Term frequency in corpus: " + temp[2]);
                        System.out.println("Inverted list offset: " + temp[1]);

                        reader.close(); // Close term_info.txt reader
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Could not read file" + termInfoFile.getAbsolutePath());
            }
        }
    }


    /*
     * Function returning metadata for given term in given document
     */
    public void getTermInDoc (String term, String filename, File docIdFile, File termsIdFile, File termIndexFile,
            File termInfoFile) {

        String stemmed = term;
        try {
            stemmed = EnglishSnowballStemmerFactory.getInstance().process(term); // Apply snoball stemmer for stemming
                                                                                 // terms
        } catch (StemmerException e) {
            System.out.println("Stemming failed for term: " + term);
        }

        String termId = new Utilities().getID(termsIdFile, stemmed); // Get the term id corresponding to given term

        String docId = new Utilities().getID(docIdFile, filename); // Get the document ID corresponding to document name

        if (docId != null && termId != null) {
            long offset = 0;

            String line;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(termInfoFile));// Read termsinfo.txt

                // Iterate through terms_info.txt to find term_index.txt offset for given term id
                while ((line = reader.readLine()) != null) {
                    String[] temp = line.split(seperator);

                    if (temp[0].equals(termId)) {
                        offset = Long.parseLong(temp[1]);
                        break;
                    }
                }
                reader.close(); // Close term_info.txt reader

            } catch (IOException e) {
                System.err.println("Could not read file" + termInfoFile.getAbsolutePath());
            }

            try {
                RandomAccessFile indexFile = new RandomAccessFile(termIndexFile, "r"); // Randomly access terms_index
                                                                                       // file
                indexFile.seek(offset);

                line = indexFile.readLine(); // Read line at the given offset
                String[] termOcurrence = line.split(seperator); // Split to get tokens
                String[] docOcurrence;
                ArrayList<Integer> positions = new ArrayList<Integer>();

                int docCount = 0;
                int count = 0;

                for (int i = 1; i < termOcurrence.length; i++) {
                    docOcurrence = termOcurrence[i].split(":"); // Split document:position pair
                    docCount += Integer.parseInt(docOcurrence[0]); // Increment document id in this case count
                    if (docCount != Integer.parseInt(docId)) // Doc id does not match continue
                        continue;
                    else { // Traversing positions for required document build position list
                        count += Integer.parseInt(docOcurrence[1]);
                        positions.add(count);
                    }
                    if (docCount > Integer.parseInt(docId)) // Already found the document skip traversing remaining
                                                            // documents for the term
                        break;
                }

                indexFile.close(); // Close term_index.txt reader

                // Print metadata for a given term in a document
                System.out.println(newLine + "Inverted list for term: " + stemmed);
                System.out.println("In document: " + filename);
                System.out.println("TERMID: " + termId);
                System.out.println("DOCID: " + docId);
                System.out.println("Term frequency in document: " + positions.size());

                // Print the positions of a term in a document if not empty else display custom message
                if (!positions.isEmpty()) {
                    System.out.print("Positions: ");
                    for (int i = 0; i < positions.size() - 1; i++) {
                        System.out.print(positions.get(i) + ", ");
                    }
                    System.out.print(positions.get(positions.size() - 1));
                } else
                    System.out.print("Positions: Not present");
            } catch (IOException e) {
                System.err.println("Could not read file" + termIndexFile.getAbsolutePath());
            }
        }
        System.out.println();
    }

}
