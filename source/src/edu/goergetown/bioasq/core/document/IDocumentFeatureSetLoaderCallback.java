package edu.goergetown.bioasq.core.document;

/**
 * Created by yektaie on 5/16/17.
 */
public interface IDocumentFeatureSetLoaderCallback {
    boolean processDocumentFeatureSet(DocumentFeatureSet featureSet, int i, int totalDocumentCount);
}
