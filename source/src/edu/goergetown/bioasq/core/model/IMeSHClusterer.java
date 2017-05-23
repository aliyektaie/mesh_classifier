package edu.goergetown.bioasq.core.model;

import edu.goergetown.bioasq.core.document.Document;

import java.util.ArrayList;

/**
 * Created by Yektaie on 5/19/2017.
 */
public interface IMeSHClusterer {
    int numberOfIteration();

    int numberOfCluster();

    Vector createMeSHVector(Document document);

    int getNumberOfThreads();

    String getName();

    String getSavingDestinationPath();

    void classifyVector(Vector vector, ArrayList<Cluster> clusters);

    double getSimilarity(Vector v1, Vector v2);

    void initializeClusteringIteration(ArrayList<Cluster> clusters);
}
