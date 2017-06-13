package edu.goergetown.bioasq.core.model.classification;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.Guid;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.document.IDocumentFeatureSetLoaderCallback;
import edu.goergetown.bioasq.core.model.Cluster;
import edu.goergetown.bioasq.core.model.FeatureValuePair;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.BinaryBuffer;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/29/2017.
 */
public abstract class MeSHClassificationModelBase {

    public static boolean exists(ClassifierParameter parameter) {
        return FileUtils.exists(getModelPath(parameter));
    }

    private static String getModelPath(ClassifierParameter parameter) {
        return Constants.CLASSIFIERS_MODELS_FOLDER + parameter.toString() + ".model";
    }

    public static void create(ITaskListener listener, SubTaskInfo task, ClassifierParameter parameter) {
        ArrayList<Vector> documentsFeatures = loadDocuments(listener, parameter);
        ArrayList<Cluster> clusters = loadClusters(listener, parameter);
        ArrayList<ClassificationCluster> termClusters = convertClustersFromDocumentsToTerms(listener, clusters, createDocumentMapper(documentsFeatures), parameter);

        saveFile(listener, termClusters, parameter);
    }

    private static void saveFile(ITaskListener listener, ArrayList<ClassificationCluster> termClusters, ClassifierParameter parameter) {
        listener.log("Saving model file");
        try {
            FileOutputStream fs = new FileOutputStream(getModelPath(parameter));
            writeInt(fs, termClusters.size());

            int i = 0;
            for (ClassificationCluster cluster : termClusters) {
                i++;
                if (i % 10 == 0)
                    listener.setProgress(i, termClusters.size());


                byte[] data = cluster.serialize();
                writeInt(fs, data.length);
                fs.write(data);
            }

            fs.flush();
            fs.close();
        } catch (Exception e) {

        }
    }

    private static void writeInt(FileOutputStream fs, int size) throws IOException {
        BinaryBuffer buffer = new BinaryBuffer(4);
        buffer.append(size);

        fs.write(buffer.getBuffer());
    }


    private static ArrayList<ClassificationCluster> convertClustersFromDocumentsToTerms(ITaskListener listener, ArrayList<Cluster> clusters, Hashtable<String, Vector> documentsFeatures, ClassifierParameter parameter) {
        listener.log("Creating term clusters");
        ArrayList<ClassificationCluster> result = new ArrayList<>();

        for (int i = 0; i < clusters.size(); i++) {
            if (i % 10 == 0) {
                listener.setProgress(i, clusters.size());
            }

            ClassificationCluster cCluster = new ClassificationCluster();
            cCluster.identifier = Guid.NewGuid();
            result.add(cCluster);

            Cluster cluster = clusters.get(i);
            for (int j = 0; j < cluster.centroid.featureCount(); j++) {
                FeatureValuePair pair = cluster.centroid.getFeature(j);

                cCluster.meshes.add(new ClassificationClusterMeSH(pair.name, pair.value));
            }

            ArrayList<Vector> documents = cluster.vectors;
            Vector centroid = new Vector();
            centroid.checkForAddDuplicates();

            for (Vector document : documents) {
                Vector features = documentsFeatures.get(document.identifier);
                for (int j = 0; j < features.featureCount(); j++) {
                    FeatureValuePair f = features.getFeature(j);
                    centroid.addWeight(f.name, f.value);
                }
            }

            centroid.optimize();
            cCluster.centroid = centroid;
        }

        return result;
    }

    private static Hashtable<String, Vector> createDocumentMapper(ArrayList<Vector> documentsFeatures) {
        Hashtable<String, Vector> result = new Hashtable<>();

        for (Vector vector : documentsFeatures) {
            result.put(vector.identifier, vector);
        }

        return result;
    }

    public static ArrayList<Vector> loadDocuments(ITaskListener listener, ClassifierParameter parameter) {
        listener.log("Loading feature set [" + parameter.termExtractionMethod + "]");
        ArrayList<String> files = FileUtils.getFiles(Constants.TRAIN_DOCUMENT_FEATURES_DATA_FOLDER + parameter.termExtractionMethod + Constants.BACK_SLASH);
        ArrayList<Vector> result = new ArrayList<>();
        int totalCount = 0;

        for (String file : files) {
            totalCount += DocumentFeatureSet.getDocumentFeatureSetCountInFile(file);
        }

        for (String file : files) {
            int finalTotalCount = totalCount;
            DocumentFeatureSet.iterateOnDocumentFeatureSetListFile(file, new IDocumentFeatureSetLoaderCallback() {
                @Override
                public boolean processDocumentFeatureSet(DocumentFeatureSet featureSet, int i, int totalDocumentCount) {
                    result.add(featureSet.toVector());

                    if (result.size() % 1000 == 0) {
                        listener.setProgress(result.size(), finalTotalCount);
                    }

                    return true;
                }
            });
        }

        return result;
    }

    public static ArrayList<Cluster> loadClusters(ITaskListener listener, ClassifierParameter parameter) {
        String filePathTemplate = Constants.CLUSTERING_DOCUMENT_MESH_INFO_FOLDER + parameter.clusteringMethod + Constants.BACK_SLASH + parameter.clusteringParameter + Constants.BACK_SLASH + "%d" + Constants.BACK_SLASH + "clusters.bin";
        int iteration = 1;

        while (FileUtils.exists(String.format(filePathTemplate, iteration))) {
            iteration++;
        }

        iteration--;
        String path = String.format(filePathTemplate, iteration);

        return Cluster.loadClusters(path, listener);
    }

    public static ArrayList<ClassificationCluster> load(ITaskListener listener, ClassifierParameter parameter) {
        ArrayList<ClassificationCluster> result = new ArrayList<>();
        String path = getModelPath(parameter);

        try {
            FileInputStream fs = new FileInputStream(new File(path));
            int count = readIntFromFile(fs);

            for (int i = 0; i < count; i++) {
                if (i % 10 == 0)
                    listener.setProgress(i, count);

                ClassificationCluster cluster = new ClassificationCluster();
                int length = readIntFromFile(fs);
                byte[] data = new byte[length];

                fs.read(data);
                cluster.load(data);

                result.add(cluster);
            }

            fs.close();
            listener.log(String.format("%d clusters loaded from \"%s\"", count, path));
        } catch (Exception e) {

        }

        return result;
    }

    private static int readIntFromFile(FileInputStream fs) throws IOException {
        byte[] data = new byte[4];
        fs.read(data);

        BinaryBuffer buffer = new BinaryBuffer(data);
        return buffer.readInt();
    }

    public static String getModelHash(ITaskListener listener, ClassifierParameter parameter) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(Paths.get(getModelPath(parameter)));
                 DigestInputStream dis = new DigestInputStream(is, md)) {

                int length = 1024 * 1024;
                byte[] data = new byte[length];
                while (dis.available() > 0) {
                    dis.read(data);
                }
            } catch (Exception ex) {

            }
            byte[] digest = md.digest();
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
        }
        return "";
    }
}
