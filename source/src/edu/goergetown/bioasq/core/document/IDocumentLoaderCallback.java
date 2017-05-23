package edu.goergetown.bioasq.core.document;

/**
 * Created by yektaie on 5/15/17.
 */
public interface IDocumentLoaderCallback {
    void processDocument(Document document, int i, int totalDocumentCount);
}
