package edu.goergetown.bioasq.core.ml.implementations;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterGrid;
import edu.berkeley.compbio.jlibsvm.SolutionModel;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.binary.C_SVC;
import edu.berkeley.compbio.jlibsvm.binary.MutableBinaryClassificationProblemImpl;
import edu.berkeley.compbio.jlibsvm.kernel.LinearKernel;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;
import edu.goergetown.bioasq.core.ml.EvaluationOnTrainingDataResult;
import edu.goergetown.bioasq.core.ml.IMachineLearningTrainer;
import edu.goergetown.bioasq.utils.FileUtils;
import edu.goergetown.bioasq.utils.ObjectSerializer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Yektaie on 6/4/2017.
 */
public class SupportVectorMachineTrainer implements IMachineLearningTrainer {

    @Override
    public String getTitle() {
        return "Linear SVM";
    }

    @Override
    public String getFileExtension() {
        return ".svm";
    }

    @Override
    public EvaluationOnTrainingDataResult train(double[][] input, double[][] output, int featureCount, String savePath) {
        boolean allSame = true;
        double first = output[0][0];

        for (int i = 1; i < output.length; i++) {
            if (first != output[i][0]) {
                allSame= false;
                break;
            }
        }

        if (allSame) {
            EvaluationOnTrainingDataResult result = new EvaluationOnTrainingDataResult();
            result.wrong = 1;
            return result;
        }

        C_SVC svm = new C_SVC();
        ImmutableSvmParameterGrid.Builder builder = ImmutableSvmParameterGrid.builder();

        HashSet<Float> cSet;
        HashSet<LinearKernel> kernelSet;

        cSet = new HashSet<>();
        cSet.add(1.0f);

        kernelSet = new HashSet<>();
        kernelSet.add(new LinearKernel());

        builder.eps = 0.001f; // epsilon
        builder.Cset = cSet; // C values used
        builder.kernelSet = kernelSet; //Kernel used

        ImmutableSvmParameter params = builder.build();

        ArrayList<SparseVector> vectors = new ArrayList<>();
        for (int i = 0; i < input.length; i++) {
            vectors.add(toSparseVector(input[i]));
        }
        MutableBinaryClassificationProblemImpl problem = new MutableBinaryClassificationProblemImpl(String.class, vectors.size());

        for (int i = 0; i < vectors.size(); i++) {
            problem.addExample(vectors.get(i), getOutput(output[i][0]));
        }

        BinaryModel model = svm.train(problem, params);
        saveModel(savePath, model);
        model = loadModel(savePath);

        EvaluationOnTrainingDataResult result = new EvaluationOnTrainingDataResult();

        for (int i = 0; i < vectors.size(); i++) {
            String str = model.predictLabel(vectors.get(i)).toString();
            boolean prediction = Boolean.valueOf(str);
            boolean correct = Boolean.valueOf(getOutput(output[i][0]));

            if (correct == prediction) {
                result.correct++;
            } else {
                result.wrong++;
            }
        }

        return result;
    }

    private BinaryModel loadModel(String savePath) {
        return (BinaryModel)ObjectSerializer.load(FileUtils.readBinaryFile(savePath));
    }

    @Override
    public boolean evaluate(double[] features, String modelPath) {
        BinaryModel model = (BinaryModel) SolutionModel.identifyTypeAndLoad(modelPath);
        int label = (int) model.predictLabel(toSparseVector(features));
        return label == 1;
    }

    private void saveModel(String path, BinaryModel model) {
        byte[] data = ObjectSerializer.serialize(model);
        FileUtils.writeBinary(path, data);
    }

    private String getOutput(double value) {
        if (value == 1) {
            return "true";
        }

        return "false";
    }

    private SparseVector toSparseVector(double[] features) {
        SparseVector sparseVector = new SparseVector(features.length);
        int[] indices = new int[features.length];
        float[] values = new float[features.length];
        for (int i = 0; i < features.length; i++) {
            indices[i] = new Integer(i);
            values[i] = (float) features[i];
        }
        sparseVector.indexes = indices;
        sparseVector.values = values;
        return sparseVector;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}