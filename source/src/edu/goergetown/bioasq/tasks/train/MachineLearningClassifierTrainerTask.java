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

/**
 * Created by Yektaie on 6/3/2017.
 */
public class MachineLearningClassifierTrainerTask extends BaseTask {
    private static IMachineLearningTrainer NEURAL_NETWORK_TRAINER = new NeuralNetworkTrainer();
    private static IMachineLearningTrainer SVM_TRAINER = new SupportVectorMachineTrainer();

    private SubTaskInfo TRAINER_TASK = new SubTaskInfo("", 1000);
    private IMachineLearningTrainer trainer = NEURAL_NETWORK_TRAINER;

    private String LIST_OF_INPUT_FILE_TO_TRAIN = "d:\\file1.txt";

    @Override
    public void process(ITaskListener listener) {
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
    private boolean finished = false;
    private double progress = 0;
    public ArrayList<String> files = new ArrayList<>();
    public ITaskListener listener;
    public IMachineLearningTrainer trainer;

    @Override
    public void run() {
        for (int i = 0; i < files.size(); i++) {
            progress = i * 100.0 / files.size();

            String[] lines = FileUtils.readAllLines(files.get(i));
            int inputCount = lines[0].split(" ").length - 1;

            double[][] input = new double[lines.length][];
            double[][] output = new double[lines.length][];
            populateInputsAndOutputs(lines, inputCount, input, output);

            String mesh = files.get(i);
            mesh = mesh.substring(mesh.lastIndexOf(Constants.BACK_SLASH) + 1);
            mesh = mesh.replace(".data", "");

            String savePath = Constants.TEXT_CLASSIFIER_FOLDER + trainer.getTitle() + Constants.BACK_SLASH + mesh + trainer.getFileExtension();
            EvaluationOnTrainingDataResult trainResult = trainer.train(input, output, inputCount, savePath);

            double accuracy = trainResult.correct;
            accuracy = accuracy / (trainResult.correct + trainResult.wrong);
            listener.logWithoutTime(String.format("%s,%d,%d,%.3f", mesh, trainResult.correct, trainResult.wrong, accuracy));
        }

        finished = true;
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