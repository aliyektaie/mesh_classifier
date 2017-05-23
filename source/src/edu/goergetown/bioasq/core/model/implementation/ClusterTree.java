package edu.goergetown.bioasq.core.model.implementation;

import edu.goergetown.bioasq.core.model.Cluster;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.utils.Matrix;
import edu.goergetown.bioasq.utils.MatrixElement;

import java.util.ArrayList;

/**
 * Created by yektaie on 5/19/17.
 */
public class ClusterTree {
    private ClusterNode _root = new ClusterNode();

    private ClusterTree() {

    }

    public Cluster findClosest(Vector vector) {
        return findClosest(_root, vector);
    }

    private Cluster findClosest(ClusterNode parent, Vector vector) {
        ClusterNode child = null;
        double similarity = -2;

        for (ClusterNode node : parent.children) {
            double sim = getSimilarity(node.centroid, vector);
            if (sim > similarity) {
                child = node;
                similarity = sim;
            }
        }

        if (child.children.size() > 0) {
            return findClosest(child, vector);
        }

        return child.cluster;
    }

    public static ClusterTree build(ArrayList<Cluster> clusters) {
        ArrayList<ClusterNode> nodes = new ArrayList<>();
        for (Cluster cluster : clusters) {
            ClusterNode node = new ClusterNode();

            node.centroid = cluster.centroid;
            node.centroid.optimize();
            node.cluster = cluster;

            nodes.add(node);
        }

        while (nodes.size() > 8) {
            nodes = clusterNodes(nodes);
        }

        ClusterTree result = new ClusterTree();
        result._root.children.addAll(nodes);

        return result;
    }

    private static ArrayList<ClusterNode> clusterNodes(ArrayList<ClusterNode> nodes) {
        ArrayList<ClusterNode> result = new ArrayList<>();

        Matrix matrix = new Matrix(nodes.size(), nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < i; j++) {
                ClusterNode node1 = nodes.get(i);
                ClusterNode node2 = nodes.get(j);

                matrix.set(i,j, getSimilarity(node1.centroid, node2.centroid), true);
            }
        }

        for (int i = 0; i < nodes.size() / 2; i++) {
            MatrixElement element = matrix.getMaximumElement(true);
            ClusterNode child1 = nodes.get(element.row);
            ClusterNode child2 = nodes.get(element.column);

            ClusterNode node = new ClusterNode();
            node.centroid = child1.centroid.add(child2.centroid);
            node.centroid.optimize();
            node.children.add(child1);
            node.children.add(child2);

            nullifyChildInMatrix(matrix, element.row, nodes.size());
            nullifyChildInMatrix(matrix, element.column, nodes.size());

            result.add(node);
        }

        return result;
    }

    private static void nullifyChildInMatrix(Matrix matrix, int index, int count) {
        for (int i = 0; i < count; i++) {
            matrix.set(index, i, -2, true);
        }
    }

    private static double getSimilarity(Vector node1, Vector node2) {
        return node1.cosine(node2);
    }
}

class ClusterNode {
    public Vector centroid = null;
    public Cluster cluster = null;

    public ArrayList<ClusterNode> children = new ArrayList<>();
}