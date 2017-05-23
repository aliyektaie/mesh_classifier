package edu.goergetown.bioasq.core.document;

/**
 * Created by Yektaie on 5/11/2017.
 */
public class Term {
    public String token = "";
    public String partOfSpeech = "";
    private String stemmed = null;

    @Override
    public String toString() {
        return token + "/" + partOfSpeech;
    }

    public String getStemmed() {
        if (stemmed == null) {
            Stemmer stemmer = new Stemmer();
            stemmed = stemmer.stem(token);
        }

        return stemmed;
    }
}
