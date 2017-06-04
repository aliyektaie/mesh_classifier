package edu.goergetown.bioasq.core.predictor;

import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.model.classification.ClassificationCluster;

import java.util.ArrayList;

/**
 * Created by Yektaie on 5/31/2017.
 */
public interface IMeshPredictor {
    ArrayList<String> predict(DocumentFeatureSet document, ClassificationCluster cluster);

    IMeshPredictor duplicate();

    String getReportFolderName();
}
