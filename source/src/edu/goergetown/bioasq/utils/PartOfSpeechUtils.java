package edu.goergetown.bioasq.utils;

import java.util.ArrayList;

/**
 * Created by Yektaie on 6/2/2017.
 */
public class PartOfSpeechUtils {
    public static ArrayList<String> getPartOfSpeechToBeConsidered() {
        ArrayList<String> list = new ArrayList<>();

        list.add("NNPS");
        list.add("JJ");
        list.add("FW");
        list.add("JJR");
        list.add("NNS");
        list.add("NNP");
        list.add("NN");

        return list;
    }
}
