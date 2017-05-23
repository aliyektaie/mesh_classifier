package edu.goergetown.bioasq.core.feature_extractor.implementations;

import java.util.ArrayList;

/**
 * Created by yektaie on 5/17/17.
 */
public class NounPhrase {
    public ArrayList<String> phrase = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> locations = new ArrayList<>();

    public NounPhrase() {
        locations.add(new ArrayList<>());
        phrase.add("");
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < phrase.size(); i++) {
            result.append(phrase.get(i));
            result.append(" -> {");

            String del = "";
            for (int location : locations.get(i)) {
                result.append(del);
                result.append(location);

                del = ", ";
            }

            result.append("}\n");
        }

        return result.toString().trim();
    }
}
