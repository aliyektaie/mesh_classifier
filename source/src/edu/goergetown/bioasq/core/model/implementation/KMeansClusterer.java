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
public class KMeansClusterer implements IMeSHClusterer {
    private int numberOfClusters = 800;
    private ClusterTree clusterTree = null;

    public KMeansClusterer(int numberOfClusters) {
        this.numberOfClusters = numberOfClusters;
    }

    @Override
    public int numberOfIteration() {
        return 20;
    }

    @Override
    public int numberOfCluster() {
        return numberOfClusters;
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

    private double scale(double probability) {
        return (probability - MeSHProbabilityDistribution.getMinimumProbability()) / (MeSHProbabilityDistribution.getMaximumProbability() - MeSHProbabilityDistribution.getMinimumProbability());
    }

    @Override
    public int getNumberOfThreads() {
        return Constants.CORE_COUNT;
    }

    @Override
    public String getName() {
        return "K Means";
    }

    @Override
    public String getSavingDestinationPath() {
        return Constants.CLUSTERING_DOCUMENT_MESH_INFO_FOLDER + "k-means" + Constants.BACK_SLASH + numberOfCluster() + Constants.BACK_SLASH;
    }

    @Override
    public void classifyVector(Vector vector, ArrayList<Cluster> clusters) {
        double sim = -2;
        Cluster targetCluster = null;

        for (Cluster cluster : clusters) {
            double s = getSimilarity(cluster.getCentroid(), (vector));
            if (s > sim) {
                sim = s;
                targetCluster = cluster;
            }
        }

        if (targetCluster != null) {
            targetCluster.addVector(vector, false);
        }

//        Cluster cluster = clusterTree.findClosest(vector);
//        if (cluster != null) {
//            cluster.addVector(vector, false);
//        }
    }

    public double getSimilarity(Vector centroid, Vector vector) {
        return centroid.cosine(vector);
    }

    @Override
    public void initializeClusteringIteration(ArrayList<Cluster> clusters) {
        clusterTree = ClusterTree.build(clusters);
    }
}
