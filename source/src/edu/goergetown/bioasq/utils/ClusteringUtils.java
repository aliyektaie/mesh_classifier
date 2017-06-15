package edu.goergetown.bioasq.utils;

import edu.goergetown.bioasq.core.document.Document;
import edu.goergetown.bioasq.core.mesh.MeSHProbabilityDistribution;
import edu.goergetown.bioasq.core.model.Vector;

/**
 * Created by zghasemi on 6/13/2017.
 */
public class ClusteringUtils {
    public static Vector createMeSHVector(Document document) {
        Vector result = new Vector();
        result.identifier = String.format("year: %s; pmid: %s", String.valueOf(document.metadata.get("year")), document.identifier);

        double p_min = MeSHProbabilityDistribution.getMinimumProbability();
        double p_max = MeSHProbabilityDistribution.getMaximumProbability();
        double scale_min = 0.5;

        for (String mesh : document.categories) {
            double probability = MeSHProbabilityDistribution.getProbability(mesh);
            probability = scale_min + (1 - scale_min) * ((probability - p_min) / (p_max - p_min));

            result.addWeight(mesh, 0.5 + (1.0 - probability));
        }

        result.optimize();

        return result;
    }
}
