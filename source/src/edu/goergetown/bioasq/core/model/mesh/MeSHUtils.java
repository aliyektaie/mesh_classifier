package edu.goergetown.bioasq.core.model.mesh;

import java.util.ArrayList;

/**
 * Created by Yektaie on 5/22/2017.
 */
public class MeSHUtils {
    private static ArrayList<String> listOfCheckTags = null;

    public static ArrayList<String> getCheckTagsMeSH() {
        if (listOfCheckTags == null) {
            listOfCheckTags = new ArrayList<>();

            listOfCheckTags.add("Adolescent");
            listOfCheckTags.add("Adult");
            listOfCheckTags.add("Aged");
            listOfCheckTags.add("Aged, 80 and over");
            listOfCheckTags.add("Humans");
            listOfCheckTags.add("Infant");
            listOfCheckTags.add("Child");
            listOfCheckTags.add("Male");
            listOfCheckTags.add("Child, Preschool");
            listOfCheckTags.add("Middle Aged");
            listOfCheckTags.add("Female");
            listOfCheckTags.add("Swine");
            listOfCheckTags.add("Adult");
        }

        return listOfCheckTags;
    }
}
