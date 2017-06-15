package edu.goergetown.bioasq.tasks.train;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.ml.EvaluationOnTrainingDataResult;
import edu.goergetown.bioasq.core.ml.IMachineLearningTrainer;
import edu.goergetown.bioasq.core.ml.implementations.*;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

/**
 * Created by Yektaie on 6/3/2017.
 */
public class MachineLearningClassifierTrainerTask extends BaseTask {
    private static IMachineLearningTrainer NEURAL_NETWORK_TRAINER = new NeuralNetworkTrainer();
    private static IMachineLearningTrainer SVM_TRAINER = new SupportVectorMachineTrainer();
    private static IMachineLearningTrainer NB_TRAINER = new NaiveBayesTrainer();
    private static IMachineLearningTrainer VS_TRAINER = new VectorSimilarityTrainer();

    private SubTaskInfo TRAINER_TASK = new SubTaskInfo("", 1000);
    private IMachineLearningTrainer trainer = NEURAL_NETWORK_TRAINER;

    private String LIST_OF_INPUT_FILE_TO_TRAIN = "C:\\Users\\zghasemi\\Desktop\\Ali\\data\\Text Classifiers\\mesh-list-1.txt";

    @Override
    public void process(ITaskListener listener) {
        String outFolder = Constants.TEXT_CLASSIFIER_FOLDER + trainer.getTitle();
        if (!FileUtils.exists(outFolder))
            FileUtils.createDirectory(outFolder);

        ArrayList<String> files = getTrainingFiles(listener);
        ArrayList<ISubTaskThread> threads = new ArrayList<>();

        TRAINER_TASK.title = String.format("Training %s", trainer.getTitle());
        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            TrainerThread t = new TrainerThread();
            threads.add(t);

            t.files = new ArrayList<>();
            t.listener = listener;
            t.trainer = trainer;
            for (int j = i; j < files.size(); j += Constants.CORE_COUNT) {
                t.files.add(files.get(j));
            }
        }

        executeWorkerThreads(listener, TRAINER_TASK, threads);
    }

    @Override
    protected String getTaskClass() {
        return CLASS_TRAIN;
    }

    @Override
    protected String getTaskTitle() {
        return "Machine Learning Trainer for MeSH Binary Classifier";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(TRAINER_TASK);

        return result;
    }

    private ArrayList<String> getTrainingFiles(ITaskListener listener) {
        ArrayList<String> result = new ArrayList<>();
        ArrayList<String> files = getDataFiles();

        for (int i = 0; i < files.size(); i++) {
            if (i % 200 == 0) {
                listener.setProgress(i, files.size());
            }

            String path = files.get(i);
            if (path.endsWith(".data")) {
                String nn = path.replace(".data", trainer.getFileExtension());
                nn = Constants.TEXT_CLASSIFIER_FOLDER + trainer.getTitle() + nn.substring(nn.lastIndexOf(Constants.BACK_SLASH));

                if (!FileUtils.exists(nn)) {
                    result.add(path);
                }
            }
        }

        return result;
    }

    private ArrayList<String> getDataFiles() {
        String featuresFolder = Constants.TEXT_CLASSIFIER_FOLDER + "Features" + Constants.BACK_SLASH;
        if (LIST_OF_INPUT_FILE_TO_TRAIN == null) {
            return FileUtils.getFiles(featuresFolder);
        }

        String[] files = FileUtils.readAllLines(LIST_OF_INPUT_FILE_TO_TRAIN);
        ArrayList<String> result = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            result.add(featuresFolder + files[i] + ".data");
        }

        return result;
    }

    @Override
    public Hashtable<String, ArrayList<Object>> getParameters() {
        Hashtable<String, ArrayList<Object>> result = new Hashtable<>();

        ArrayList<Object> trainers = new ArrayList<>();
        trainers.add(NEURAL_NETWORK_TRAINER);
        trainers.add(SVM_TRAINER);
        trainers.add(NB_TRAINER);
        trainers.add(VS_TRAINER);
        result.put("trainers", trainers);

        return result;
    }

    @Override
    public Object getParameter(String name) {
        return trainer;
    }

    @Override
    public void setParameter(String name, Object value) {
        if (value != null) {
            trainer = (IMachineLearningTrainer) value;
        }
    }
}

class TrainerThread extends Thread implements ISubTaskThread {
    private static final int SAMPLE_COUNT = 800000;
    private static final int SAMPLE_COUNT_INCREASE = 4000;
    private boolean finished = false;
    private double progress = 0;
    public ArrayList<String> files = new ArrayList<>();
    public ITaskListener listener;
    public IMachineLearningTrainer trainer;

    @Override
    public void run() {
        for (int i = 0; i < files.size(); i++) {
            progress = i * 100.0 / files.size();

            String[] vocabulary = FileUtils.readAllLines(files.get(i).replace(".data", ".txt"));
            String[] fileContent = convertSparceToMatrix(FileUtils.readAllLines(files.get(i)), vocabulary);
            int iteration = 0;

            while (iteration < 100 && (SAMPLE_COUNT + iteration * SAMPLE_COUNT_INCREASE) < fileContent.length || fileContent.length < SAMPLE_COUNT) {
                String[] lines = sampleData(fileContent, SAMPLE_COUNT + iteration * SAMPLE_COUNT_INCREASE);
                int inputCount = lines[0].split(" ").length - 1;

                double[][] input = new double[lines.length][];
                double[][] output = new double[lines.length][];
                populateInputsAndOutputs(lines, inputCount, input, output);

                String mesh = files.get(i);
                mesh = mesh.substring(mesh.lastIndexOf(Constants.BACK_SLASH) + 1);
                mesh = mesh.replace(".data", "");

                String savePath = Constants.TEXT_CLASSIFIER_FOLDER + trainer.getTitle() + Constants.BACK_SLASH + mesh + trainer.getFileExtension();
                EvaluationOnTrainingDataResult trainResult = trainer.train(input, vocabulary, output, inputCount, savePath);

                double accuracy = trainResult.correct;
                accuracy = accuracy / (trainResult.correct + trainResult.wrong);

                if (accuracy > 0.9 || fileContent.length < SAMPLE_COUNT) {
                    listener.logWithoutTime(String.format("%s,%d,%d,%.3f", mesh, trainResult.correct, trainResult.wrong, accuracy));
                    if (iteration > 0)
                        listener.logWithoutTime("----------------------------------------------------------------");
                    break;
                } else {
                    listener.logWithoutTime(String.format("%s,%d,%d,%.3f   ->   Retrying [" + iteration + "]", mesh, trainResult.correct, trainResult.wrong, accuracy));
                    iteration++;
                }
            }
        }

        finished = true;
    }

    private String[] sampleData(String[] lines, int sampleCount) {
        if (lines.length <= sampleCount) {
            return lines;
        }

        try {
            String[] result = new String[sampleCount];
            int positiveCount = getPositiveSampleCount(lines);
            int negativeCount = lines.length - positiveCount;

            int positiveCountInResult = (int) (positiveCount * (sampleCount * 1.0 / lines.length));
            int negativeCountInResult = sampleCount - positiveCountInResult;

            int[] positives = sampleIndex(lines, positiveCount, positiveCountInResult, " 1");
            int[] negatives = sampleIndex(lines, negativeCount, negativeCountInResult, " 0");

            int i1 = 0;
            int i2 = 0;
            int i = 0;

            while (i1 < positives.length && i2 < negatives.length) {
                int p = positives[i1];
                int n = negatives[i2];

                if (p > n) {
                    result[i] = lines[n];
                    i2++;
                } else {
                    result[i] = lines[p];
                    i1++;
                }

                i++;
            }

            for (int j = i1; j < positives.length; j++) {
                result[i] = lines[positives[j]];
                i++;
            }

            for (int j = i2; j < negatives.length; j++) {
                result[i] = lines[negatives[j]];
                i++;
            }

            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private String[] convertSparceToMatrix(String[] lines, String[] vocabulary) {
        String[] result = new String[lines.length];

        for (int i = 0; i < lines.length; i++) {
            result[i] = convertSparceToMatrixLine(lines[i], vocabulary.length);
        }

        return result;
    }

    private String convertSparceToMatrixLine(String line, int length) {
        StringBuilder result = new StringBuilder();
        double[] content = new double[length];
        String[] parts= line.split(" ");

        for (int i = 0; i < parts.length - 1; i++) {
            String[] a = parts[i].replace("(", "").replace(")", "").split(",");
            content[Integer.valueOf(a[0])] = Double.valueOf(a[1]);
        }

        for (int i = 0; i < length; i++) {
            result.append(content[i] == 0 ? "0" : String.format("%.5f", content[i]));
            result.append(" ");
        }

        result.append(parts[parts.length - 1].trim());

        return result.toString();
    }

    private int[] sampleIndex(String[] lines, int count, int required, String ending) {
        ArrayList<Integer> indexes = new ArrayList<>(count);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].endsWith(ending)) {
                indexes.add(i);
            }
        }

        Random random = new Random();
        int[] result = new int[required];

        for (int i = 0; i < result.length; i++) {
            int index = (random.nextInt() & 0x0fffffff) % indexes.size();
            result[i] = indexes.get(index);
            indexes.remove(index);
        }

        return result;
    }

    private int getPositiveSampleCount(String[] lines) {
        int result = 0;

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].endsWith(" 1"))
                result++;

        }
        return result;
    }

    private void populateInputsAndOutputs(String[] lines, int inputCount, double[][] input, double[][] output) {
        for (int ii = 0; ii < lines.length; ii++) {
            try {
                input[ii] = new double[inputCount];
                output[ii] = new double[1];

                String[] values = lines[ii].split(" ");
                output[ii][0] = Double.valueOf(values[values.length - 1]);

                for (int j = 0; j < inputCount; j++) {
                    input[ii][j] = Double.valueOf(values[j]);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
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