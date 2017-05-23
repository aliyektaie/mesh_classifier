package edu.goergetown.bioasq.core.model.implementation;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.mesh.MeSHProbabilityDistribution;
import edu.goergetown.bioasq.core.model.Cluster;
import edu.goergetown.bioasq.core.model.IMeSHClusterer;
import edu.goergetown.bioasq.core.model.Vector;

import java.util.ArrayList;

/**
 * Created by Yektaie on 5/19/2017.
 */
public class HierarchicalAgglomerativeClusterer implements IMeSHClusterer {
    @Override
    public int numberOfIteration() {
        return 1;
    }

    @Override
    public int numberOfCluster() {
        return 0;
    }

    @Override
    public Vector createMeSHVector(Document document) {
        Vector result = new Vector();
        result.identifier = String.format("year: %s; pmid: %s", String.valueOf(document.metadata.get("year")), document.identifier);

        for (String mesh : document.categories) {
            result.addWeight(mesh, 1.0 - scale(MeSHProbabilityDistribution.getProbability(mesh)));
        }

        result.optimize();

        return result;
    }

    private double scale(double value) {
        double min = 0.01;
        return min + ((value - MeSHProbabilityDistribution.getMinimumProbability()) * (1-min)) / (MeSHProbabilityDistribution.getMaximumProbability() - MeSHProbabilityDistribution.getMinimumProbability());
    }

    @Override
    public int getNumberOfThreads() {
        return 1;
    }

    @Override
    public String getName() {
        return "Hierarchical Agglomerative";
    }

    @Override
    public String getSavingDestinationPath() {
        return Constants.CLUSTERING_DOCUMENT_MESH_INFO_FOLDER + "hierarchical-agglomerative" + Constants.BACK_SLASH;
    }

    @Override
    public void classifyVector(Vector vector, ArrayList<Cluster> clusters) {

    }

    @Override
    public double getSimilarity(Vector v1, Vector v2) {
        return 0;
    }

    @Override
    public void initializeClusteringIteration(ArrayList<Cluster> clusters) {

    }
}
