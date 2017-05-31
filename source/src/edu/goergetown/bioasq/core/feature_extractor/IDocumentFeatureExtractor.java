package edu.goergetown.bioasq.core.feature_extractor;

import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.ui.ITaskListener;

import java.util.ArrayList;

/**
 * Created by yektaie on 5/15/17.
 */
public interface IDocumentFeatureExtractor {
    String getDestinationFolderName();

    String getTitle();

    boolean needPreprocessTask();

    int getPreprocessTaskGetTimeInMinutes();

    void preprocess(ITaskListener listener);

    DocumentFeatureSet extractFeatures(Document document);

    void setInputFiles(ArrayList<String> inputFiles);
}
