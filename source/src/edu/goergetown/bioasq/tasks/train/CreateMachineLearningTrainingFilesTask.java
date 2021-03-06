package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.document.DocumentFeatureSet;
import edu.goergetown.bioasq.core.document.IDocumentFeatureSetLoaderCallback;
import edu.goergetown.bioasq.core.model.Cluster;
import edu.goergetown.bioasq.core.model.FeatureValuePair;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.model.classification.ClassificationCluster;
import edu.goergetown.bioasq.core.model.classification.ClassifierParameter;
import edu.goergetown.bioasq.core.model.classification.MeSHClassificationModelBase;
import edu.goergetown.bioasq.core.model.mesh.MeSHUtils;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

/**
 * Created by Yektaie on 6/2/2017.
 */
public class CreateMachineLearningTrainingFilesTask extends BaseTask {
    private SubTaskInfo LOAD_VOCABULARY = new SubTaskInfo("Loading vocabulary", 0);
    private SubTaskInfo CREATE_TRAINING_FILES = new SubTaskInfo("Creating training files", 200);


    @Override
    public void process(ITaskListener listener) {
        ClassifierParameter parameter = new ClassifierParameter();
        parameter.clusteringParameter = "0.21";
        parameter.clusteringMethod = "adaptive";
        parameter.termExtractionMethod = "quick-umls";

        createFeatureFolder();

        listener.log("The sampling list file ('" + getSamplesIDFilePath() + "')");
        listener.log("This task is done in 2 steps:");
        listener.log("   1- Creating the sampling list file");
        listener.log("   2- Spliting sampling list file.");
        listener.log("   3- Creating training files based on sampling list file.");


        if (FileUtils.exists(getSamplesIDFilePath())) {
            String splited = getSplitedFolder();

            if (FileUtils.getFiles(splited).size() > 0) {
                createTrainingFiles(listener, parameter);
            } else {
                splitSamplingListFile(listener);
            }
        } else {
            createSampleISListFile(listener, parameter);
        }
    }

    private void splitSamplingListFile(ITaskListener listener) {
        ArrayList<String> list = null;
        String mesh = null;
        int count = 0;
        int index = 0;
        ArrayList<String> meshes = loadMeshList();
        ArrayList<FileOutputStream> files = new ArrayList<>();
        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            try {
                FileOutputStream fs = new FileOutputStream(getSplitedFolder() + "file-" + i + ".txt");
                files.add(fs);
            } catch (FileNotFoundException e) {

            }
        }

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(getSamplesIDFilePath())));

            String line = null;

            while ((line = br.readLine()) != null) {
                if (line.equals("-----------------------------------------------------------------")) {
                    if (mesh != null) {
                        saveSampleIDListForMesh(mesh, list, files.get(index % Constants.CORE_COUNT));
                        index++;
                        listener.setProgress(index, meshes.size());
                    }

                    if (count % 5 == 0) {
                        listener.log(count + " documents processed");
                    }

                    count++;
                    list = new ArrayList<>();
                    mesh = null;
                } else if (line.startsWith("MeSH: ")) {
                    mesh = line.substring(line.indexOf(":") + 1).trim();
                } else if (line.startsWith("\t")) {
                    String id = line.trim();
                    list.add(id);
                } else if (line.trim().equals("")) {
                }
            }
        } catch (Exception ex) {
        }

        if (mesh != null) {
            saveSampleIDListForMesh(mesh, list, files.get(index % Constants.CORE_COUNT));
        }

        for (FileOutputStream fs : files) {
            try {
                fs.flush();
                fs.close();
            } catch (IOException e) {
            }
        }
    }

    private void saveSampleIDListForMesh(String mesh, ArrayList<String> ids, FileOutputStream fs) {
        try {
            fs.write("\r\n-----------------------------------------------------------------".getBytes());
            fs.write(("\r\nMeSH: " + mesh).getBytes());
            for (String id : ids) {
                fs.write(("\r\n\t" + id).getBytes());
            }

            fs.flush();
        } catch (Exception e) {

        }
    }

    @NotNull
    private String getSplitedFolder() {
        String splited = Constants.TEMP_FOLDER + "ml-train-set-ids-splited" + Constants.BACK_SLASH;
        if (!FileUtils.exists(splited)) {
            FileUtils.createDirectory(splited);
        }
        return splited;
    }

    private void createFeatureFolder() {
        String path = Constants.TEXT_CLASSIFIER_FOLDER + "Features" + Constants.BACK_SLASH;
        if (!FileUtils.exists(path)) {
            FileUtils.createDirectory(path);
        }
    }

    private void createTrainingFiles(ITaskListener listener, ClassifierParameter parameter) {
        Hashtable<String, DocumentFeatureSet> documentsFeatures = loadDocuments(listener, parameter);
        ArrayList<String> meshes = loadMeshList();

        ArrayList<ISubTaskThread> threads = new ArrayList<>();
        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            CreateTrainingFileThread t = new CreateTrainingFileThread();
            threads.add(t);

            t.meshes.addAll(meshes);
            t.documentsFeatures.putAll(documentsFeatures);
            t.filePath = getSplitedFolder() + "file-" + i + ".txt";
        }

        executeWorkerThreads(listener, CREATE_TRAINING_FILES, threads);
    }

    public static Hashtable<String, DocumentFeatureSet> loadDocuments(ITaskListener listener, ClassifierParameter parameter) {
        listener.log("Loading feature set [" + parameter.termExtractionMethod + "]");
        ArrayList<String> files = FileUtils.getFiles(Constants.TRAIN_DOCUMENT_FEATURES_DATA_FOLDER + parameter.termExtractionMethod + Constants.BACK_SLASH);
        Hashtable<String, DocumentFeatureSet> result = new Hashtable<>();
        int totalCount = 0;

        for (String file : files) {
            totalCount += DocumentFeatureSet.getDocumentFeatureSetCountInFile(file);
        }

        for (String file : files) {
            int finalTotalCount = totalCount;
            DocumentFeatureSet.iterateOnDocumentFeatureSetListFile(file, new IDocumentFeatureSetLoaderCallback() {
                @Override
                public boolean processDocumentFeatureSet(DocumentFeatureSet featureSet, int i, int totalDocumentCount) {
                    String identifier = String.format("year: %s; pmid: %s", String.valueOf(featureSet.documentYear), featureSet.documentIdentifier);
                    result.put(identifier, featureSet);

                    if (result.size() % 1000 == 0) {
                        listener.setProgress(result.size(), finalTotalCount);
                    }

                    return true;
                }
            });
        }

        return result;
    }


    private void createSampleISListFile(ITaskListener listener, ClassifierParameter parameter) {
        listener.log("Performing Step 1:");
        listener.log("   -> Loading set document features");

        ArrayList<Cluster> clusters = MeSHClassificationModelBase.loadClusters(listener, parameter);
        listener.log("   -> Clusters loaded");
        ArrayList<String> meshes = loadMeshList();
        listener.log("   -> MeSH list loaded");

        listener.log("   -> Sampling clusters");
        LOAD_VOCABULARY.estimation = 0;
        Hashtable<String, ArrayList<String>> samples = doSampling(clusters, meshes, listener);
        listener.log("   -> Sampling done, saving file");
        listener.setCurrentState("Sampling done, saving file");

        saveSampleIDList(listener, samples);
    }

    private void saveSampleIDList(ITaskListener listener, Hashtable<String, ArrayList<String>> samples) {
        int count = 0;
        int i = 0;

        try {
            FileOutputStream fs = new FileOutputStream(getSamplesIDFilePath());

            for (String mesh : samples.keySet()) {
                ArrayList<String> ids = samples.get(mesh);
                if (i % 10 == 0) {
                    listener.setProgress(i, samples.size());
                }
                count += ids.size();

                fs.write("\r\n-----------------------------------------------------------------".getBytes());
                fs.write(("\r\nMeSH: " + mesh).getBytes());
                for (String id : ids) {
                    fs.write(("\r\n\t" + id).getBytes());
                }

                i++;
            }

            fs.flush();
            fs.close();

            listener.log("Average sample per set: " + (count * 1.0 / samples.size()));
        } catch (Exception e) {

        }
    }

    private String getSamplesIDFilePath() {
        return Constants.TEMP_FOLDER + "ml-train-set-ids.txt";
    }

    private Hashtable<String, ArrayList<String>> doSampling(ArrayList<Cluster> model, ArrayList<String> meshes, ITaskListener listener) {
        ArrayList<ISubTaskThread> threads = new ArrayList<>();
        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            SamplerThread t = new SamplerThread();
            threads.add(t);

            t.meshes = new ArrayList<>();
            for (int j = i; j < meshes.size(); j += Constants.CORE_COUNT) {
                t.meshes.add(meshes.get(j));
            }

            t.model = cloneClusters(model);
        }

        executeWorkerThreads(listener, CREATE_TRAINING_FILES, threads);

        Hashtable<String, ArrayList<String>> result = new Hashtable<>();
        for (ISubTaskThread t : threads) {
            result.putAll(((SamplerThread) t).result);
        }

        return result;
    }

    private ArrayList<Cluster> cloneClusters(ArrayList<Cluster> model) {
        ArrayList<Cluster> result = new ArrayList<>();

        result.addAll(model);

        return result;
    }

    private ArrayList<String> loadMeshList() {
        ArrayList<String> result = new ArrayList<>();

        String[] lines = FileUtils.readAllLines(MeSHTermInDocumentTextClassifierTrainerTask.getMeSHListFilePath());
        for (int i = 0; i < lines.length; i++) {
            String mesh = lines[i];

            mesh = mesh.replace("COMMMMA", ",");
            result.add(mesh);
        }

        return result;
    }


    private String[] getVocabulary(ArrayList<ClassificationCluster> model, ITaskListener listener) {
        Hashtable<String, Object> temp = new Hashtable<>();
        ArrayList<String> terms = new ArrayList<>();
        listener.setCurrentState("Extracting vocabulary");
        int ci = 0;
        for (ClassificationCluster cluster : model) {
            if (ci % 100 == 0) {
                listener.setProgress(ci, model.size());
            }
            for (int i = 0; i < cluster.centroid.featureCount(); i++) {
                FeatureValuePair feature = cluster.centroid.getFeature(i);

                if (considerToken(feature.name)) {
                    if (!temp.containsKey(feature.name)) {
                        temp.put(feature.name, new Object());
                        terms.add(feature.name);
                    }
                }
            }

            ci++;
        }

        String[] result = new String[terms.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = terms.get(i);
        }

        Arrays.sort(result);
        return result;
    }

    private boolean considerToken(String name) {
        boolean hasDigit = false;
        boolean hasLetter = false;

        name = name.toLowerCase();

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            hasDigit = hasDigit || (c >= '0' && c <= '9');
            hasLetter = hasLetter || (c >= 'a' && c <= 'z');
        }

        return hasDigit && hasLetter;
    }

    @Override
    protected String getTaskClass() {
        return CLASS_TRAIN;
    }

    @Override
    protected String getTaskTitle() {
        return "Create Machine Learning Training Files";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(LOAD_VOCABULARY);
        result.add(CREATE_TRAINING_FILES);

        return result;
    }
}

class SamplerThread extends Thread implements ISubTaskThread {
    private boolean finished = false;
    private double progress = 0.0;
    public ArrayList<String> meshes = null;
    public ArrayList<Cluster> model = null;
    public Hashtable<String, ArrayList<String>> result = new Hashtable<>();

    @Override
    public void run() {
        ArrayList<String> checkTags = MeSHUtils.getCheckTagsMeSH();

//        for (int i = 0; i < 200; i++) {
        for (int i = 0; i < meshes.size(); i++) {
            progress = i * 100.0 / meshes.size();

            String mesh = meshes.get(i);
            if (checkTags.contains(mesh))
                continue;

            ArrayList<String> samples = new ArrayList<>();
            result.put(mesh, samples);

            for (Cluster cluster : model) {
                if (cluster.centroid.containsFeature(mesh)) {
                    for (Vector vector : cluster.vectors) {
                        samples.add(vector.identifier);
                    }
                }
            }
        }


        finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public double getProgress() {
        return progress;
    }
}

class StringIntPair {
    public String str = "";
    public int number = 0;
}

class CreateTrainingFileThread extends Thread implements ISubTaskThread {
    private boolean isFinished = false;
    private double progress = 0;

    public ArrayList<String> meshes = new ArrayList<>();
    public String filePath = "";
    public Hashtable<String, DocumentFeatureSet> documentsFeatures = new Hashtable<>();

    @Override
    public void run() {
        ArrayList<String> list = null;
        String mesh = null;
        int count = 0;
        int index = 0;


        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));

            String line = null;

            while ((line = br.readLine()) != null) {
                if (line.equals("-----------------------------------------------------------------")) {
                    if (mesh != null) {
                        processMeshIDList(mesh, list, documentsFeatures);
                        index++;
                        progress = index*100.0 / (meshes.size() / Constants.CORE_COUNT);
                    }

                    count++;
                    list = new ArrayList<>();
                    mesh = null;
                } else if (line.startsWith("MeSH: ")) {
                    mesh = line.substring(line.indexOf(":") + 1).trim();
                } else if (line.startsWith("\t")) {
                    String id = line.trim();
                    list.add(id);
                } else if (line.trim().equals("")) {
                }
            }
        } catch (Exception ex) {
        }

        if (mesh != null) {
            processMeshIDList(mesh, list, documentsFeatures);
        }
        isFinished = true;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public double getProgress() {
        return progress;
    }

    private void processMeshIDList(String mesh, ArrayList<String> list, Hashtable<String, DocumentFeatureSet> documentsFeatures) {
        String path = Constants.TEXT_CLASSIFIER_FOLDER + "Features" + Constants.BACK_SLASH + mesh;

        if (FileUtils.exists(path + ".txt"))
            return;

        ArrayList<DocumentFeatureSet> documents = getDocuments(list, documentsFeatures);
        String[] features = getFeaturesFromDocuments(documents, mesh);


        try {
            FileOutputStream fs = new FileOutputStream(path + ".data");
            double[] weights = new double[features.length];

            StringBuilder content = new StringBuilder();

            for (DocumentFeatureSet document : documents) {
                setWeights(weights, features, document);

                for (int i = 0; i < weights.length; i++) {

//                    if (weights[i] == 0) {
//                        content.append("0");
//                    } else {
//                        content.append(String.format("%.5f", weights[i]));
//                    }
//                    content.append(" ");


                    if (weights[i] != 0) {
                        content.append(String.format("(%d,%.5f) ", i, weights[i]));
                    }
                }

                content.append(document.meshList.contains(mesh) ? "1" : "0");
                content.append("\r\n");
            }

            fs.write(content.toString().getBytes());

            fs.flush();
            fs.close();

            StringBuilder featuresFile = new StringBuilder();
            for (String feature : features) {
                featuresFile.append(feature);
                featuresFile.append("\r\n");
            }

            FileUtils.writeText(path + ".txt", featuresFile.toString());
        } catch (Exception ex) {
        }
    }

    private void setWeights(double[] weights, String[] features, DocumentFeatureSet document) {
        for (int i = 0; i < weights.length; i++) {
            weights[i] = 0;
        }

        for (String feature : document.features.keySet()) {
            int index = getIndex(features, feature);
            if (index != -1) {
                weights[index] = document.features.get(feature);
            }
        }
    }

    private int getIndex(String[] features, String feature) {

        int begin = 0;
        int end = features.length - 1;
        int middle = 0;

        feature = feature.toLowerCase();

        int c = 0;
        while (end - begin < 4 && c < 15) {
            middle = (end + begin) / 2;
            String m = features[middle];
            int cmp = m.compareTo(feature);
            if (cmp == 0) {
                return middle;
            } else if (cmp > 0) {
                end = middle;
            } else {
                begin = middle;
            }
            c++;
        }

        for (int i = begin; i <= end; i++) {
            if (features[i].equals(feature)) {
                return i;
            }
        }

        return -1;
    }

    private String[] getFeaturesFromDocuments(ArrayList<DocumentFeatureSet> documents, String mesh) {
        Hashtable<String, StringIntPair> result = new Hashtable<>();
        int positiveCount = 0;

        for (DocumentFeatureSet document : documents) {
            if (document.meshList.contains(mesh)) {
                positiveCount++;
            }

            for (String f : document.features.keySet()) {
                String feature = f.toLowerCase();
                if (!result.containsKey(feature)) {
                    StringIntPair pair = new StringIntPair();
                    pair.number = 1;
                    pair.str = feature;

                    result.put(feature, pair);
                } else {
                    result.get(feature).number++;
                }
            }
        }

        ArrayList<StringIntPair> toBeConsidered = new ArrayList<>();
        for (String feature : result.keySet()) {
            StringIntPair p = result.get(feature);

            if ((p.number > (positiveCount * 0.3)) && (p.number < (positiveCount * 1.5))) {
                toBeConsidered.add(p);
            }
        }

        String[] s = new String[toBeConsidered.size()];
        for (int i = 0; i < s.length; i++) {
            s[i] = toBeConsidered.get(i).str;
        }

        Arrays.sort(s);

        return s;
    }

    private ArrayList<DocumentFeatureSet> getDocuments(ArrayList<String> list, Hashtable<String, DocumentFeatureSet> documentsFeatures) {
        ArrayList<DocumentFeatureSet> result = new ArrayList<>();

        for (String id : list) {
            DocumentFeatureSet document = documentsFeatures.get(id);
            if (document != null) {
                result.add(document);
            }
        }

        return result;
    }

}