package edu.goergetown.bioasq.core.feature_extractor;

import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.ui.ITaskListener;

import java.util.ArrayList;

/**
 * Created by yektaie on 5/15/17.
 */
public interface IDocumentFeatureExtractor {
    String getDestinationFolder();

    String getTitle();

    boolean needNormalizationOfFeatures();

    void normalizeFeatures(ITaskListener listener, ArrayList<DocumentFeatureSet> documentsFeatureList, ArrayList<Document> documents);

    boolean needPreprocessTask();

    int getPreprocessTaskGetTimeInMinutes();

    void prepreocess(ITaskListener listener);

    DocumentFeatureSet extractFeatures(Document document);
}
