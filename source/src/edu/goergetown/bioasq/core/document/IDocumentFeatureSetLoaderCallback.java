package edu.goergetown.bioasq.core.document;

/**
 * Created by yektaie on 5/16/17.
 */
public interface IDocumentFeatureSetLoaderCallback {
    void processDocumentFeatureSet(DocumentFeatureSet featureSet, int i, int totalDocumentCount);
}
