package edu.goergetown.bioasq.core.model.mesh;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.model.Vector;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.ISubTaskThread;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.BinaryBuffer;
import edu.goergetown.bioasq.utils.FileUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/22/2017.
 */
public class MeSHGraph {
    public void createUnsortedWeightFile(ArrayList<Vector> vectors, BaseTask task, SubTaskInfo subTask, ITaskListener listener, String path) {
        long totalCount = getTotalCount(vectors);
        IJPair[] pairs = getSplitPairs(vectors);

        ArrayList<ISubTaskThread> threads = createWorkerThreads(vectors, pairs, totalCount);
        task.executeWorkerThreads(listener, subTask, threads);

        mergeResults();
    }

    private void mergeResults() {
        double[][] counts = new double[101][];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = new double[Constants.CORE_COUNT];
        }

        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            String[] lines = FileUtils.readAllLines(Constants.TEMP_FOLDER + "graph-weights-distribution-" + i + ".csv");
            for (int j = 0; j < lines.length; j++) {
                String[] parts = lines[j].split(",");
                counts[j][i] = Double.valueOf(parts[1]);
            }
        }

        StringBuilder result = new StringBuilder();

        result.append("Similarity,Core 0, Core 1, Core 2, Core 3,Sum,Accumulative Core 0,Accumulative Core 1,Accumulative Core 2,Accumulative Core 3,Accumulative Sum");
        double[] accumulative = new double[Constants.CORE_COUNT + 1];
        for (int i = 1; i < counts.length; i++) {
            result.append("\r\n");
            double value = i * 0.01;
            result.append(String.format("%.2f", value));

            double sum = 0;
            for (int j = 0; j < Constants.CORE_COUNT; j++) {
                accumulative[j] += counts[i][j];
                result.append(String.format(",%.4f", counts[i][j]));
                sum += counts[i][j];
                accumulative[Constants.CORE_COUNT] += (sum * 100 / 147.2364);

            }

            result.append(String.format(",%.4f", sum));

            for (int j = 0; j <= Constants.CORE_COUNT; j++) {
                result.append(String.format(",%.4f", accumulative[j]));
            }
        }

        FileUtils.writeText(Constants.TEMP_FOLDER + "graph-weights-distribution.csv", result.toString());
    }

    private void saveGraphWeightDistribution(ArrayList<ISubTaskThread> threads, int[] counts) {
        for (ISubTaskThread t : threads) {
            WeightCalculatorThread thread = (WeightCalculatorThread) t;
            for (int i = 0; i < counts.length; i++) {
                counts[i] += thread.counts[i];
            }
        }

        StringBuilder distribution = new StringBuilder();
        for (int i = 0; i < counts.length; i++) {
            distribution.append(i + "," + counts[i] + "\r\n");
        }

        FileUtils.writeText(Constants.TEMP_FOLDER + "graph-weights-distribution.csv", distribution.toString());
    }

    private ArrayList<ISubTaskThread> createWorkerThreads(ArrayList<Vector> vectors, IJPair[] pairs, long totalCount) {
        ArrayList<ISubTaskThread> threads = new ArrayList<>();
        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            WeightCalculatorThread thread = new WeightCalculatorThread();
            thread.domain = pairs[i];
            thread.vectors = vectors;
            thread.coreIndex = i;
            thread.totalCount = totalCount / 4;

            threads.add(thread);
        }
        return threads;
    }

    private IJPair[] getSplitPairs(ArrayList<Vector> vectors) {
        IJPair[] pairs = new IJPair[Constants.CORE_COUNT];
        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = new IJPair();
        }

        pairs[0].end_i = 0;
        pairs[0].end_i = (int) (vectors.size() * 0.5);
        pairs[1].start_i = pairs[0].end_i + 1;
        pairs[1].end_i = (int) (vectors.size() * 0.7);
        pairs[2].start_i = pairs[1].end_i + 1;
        pairs[2].end_i = (int) (vectors.size() * 0.86);
        pairs[3].start_i = pairs[2].end_i;
        pairs[3].end_i = vectors.size() - 1;

        return pairs;
    }

    private long getTotalCount(ArrayList<Vector> vectors) {
        long totalCount = 0;
        for (int i = 0; i < vectors.size(); i++) {
            for (int j = 0; j < i; j++) {
                totalCount++;
            }
        }
        return totalCount;
    }

    public void createSortedWeightFile(String unsortedPath, String sortedPath) {
        //        MeSHDistancePair[] edgesArray = new MeSHDistancePair[edges.size()];
//        for (int i = 0; i < edgesArray.length; i++)
//            edgesArray[i] = edges.get(i);
//
//        listener.setCurrentState("Sorting edges");
//        Arrays.sort(edgesArray, new Comparator<MeSHDistancePair>() {
//            @Override
//            public int compare(MeSHDistancePair o1, MeSHDistancePair o2) {
//                return new Double(o1.similarity).compareTo(o2.similarity);
//            }
//        });
//
//        saveGraphWeightsInFile(path, edgesArray);
    }

//    private void saveGraphWeightsInFile(String path, MeSHDistancePair[] edgesArray) {
//        File file = new File(path);
//        try {
//            FileOutputStream fs = new FileOutputStream(file);
//            writeInt(fs, edgesArray.length);
//
//            for (MeSHDistancePair pair : edgesArray) {
//                byte[] data = pair.serialize();
//
//                writeInt(fs, data.length);
//                fs.write(data);
//            }
//
//            fs.flush();
//            fs.close();
//        } catch (Exception e) {
////            Logger.error(e, "Unable to write file at path \"%s\".", path);
//        }
//    }
//
//    private MeSHDistancePair[] loadGraphWeightsFromFile(String path, ArrayList<Vector> vectors) {
//        try {
//            FileInputStream inputFile = new FileInputStream(path);
//            int count = FileUtils.readInteger(inputFile);
//            MeSHDistancePair[] result = new MeSHDistancePair[count];
//            Hashtable<String, Vector> map = vectorListToHashTable(vectors);
//
//            for (int i = 0; i < count; i++) {
//                int length = FileUtils.readInteger(inputFile);
//                byte[] data = new byte[length];
//
//                inputFile.read(data);
//                MeSHDistancePair m = new MeSHDistancePair();
//                m.load(data, map);
//
//                result[i] = m;
//            }
//
//            inputFile.close();
//
//            return result;
//        } catch (Exception ex) {
//
//        }
//
//        return new MeSHDistancePair[0];
//    }

    private Hashtable<String, Vector> vectorListToHashTable(ArrayList<Vector> vectors) {
        Hashtable<String, Vector> result = new Hashtable<>();

        for (Vector v : vectors) {
            result.put(v.identifier, v);
        }

        return result;
    }

}

class IJPair {
    int start_i = 0;
    int end_i = 0;
}

class WeightCalculatorThread extends Thread implements ISubTaskThread {
    private boolean finished = false;
    private double progress = 0;
    public IJPair domain = null;
    public ArrayList<Vector> vectors = null;
    public int coreIndex = 0;
    public long totalCount = 0;
    public int[] counts = new int[101];

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void run() {
        try {
            ArrayList<byte[]> datas = new ArrayList<>();
            String path = Constants.TEMP_FOLDER + "temp-create-graph-weight-" + coreIndex + ".bin";

            FileOutputStream fs = new FileOutputStream(path);

            int count = 0;

            for (int i = domain.start_i; i <= domain.end_i; i++) {
                Vector v1 = vectors.get(i);
                for (int j = 0; j < i; j++) {
                    count++;
                    progress = count * 100.0 / totalCount;

                    Vector v2 = vectors.get(j);
                    double similarity = v1.cosine(v2);
                    counts[(int) (similarity * 100)]++;

                    if (similarity > 0.9) {
                        MeSHDistancePair pair = new MeSHDistancePair();
                        pair.similarity = similarity;
                        pair.vector1 = v1;
                        pair.vector2 = v2;

                        datas.add(pair.serialize());
                        if (datas.size() == 100000) {
                            flushData(fs, datas);
                            datas.clear();
                        }
                    }
                }
            }

            flushData(fs, datas);

            fs.flush();
            fs.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        finished = true;
    }

    private void flushData(FileOutputStream fs, ArrayList<byte[]> datas) throws IOException {
        int length = 0;
        for (int i = 0; i < datas.size(); i++) {
            length += BinaryBuffer.getSerializedLength(datas.get(i));
        }

        BinaryBuffer buffer = new BinaryBuffer(length);

        for (byte[] data : datas) {
            buffer.append(data);
        }

        fs.write(buffer.getBuffer());

        StringBuilder distribution = new StringBuilder();
        double mx = 0;
        for (int i = 1; i < counts.length; i++) {
            if (mx < counts[i])
                mx = counts[i];
        }
        for (int i = 0; i < counts.length; i++) {
            distribution.append(String.format("%.2f,%.4f\r\n", i / 100.0, counts[i] / mx));
        }

        FileUtils.writeText(Constants.TEMP_FOLDER + "graph-weights-distribution-" + coreIndex + ".csv", distribution.toString());

    }

    @Override
    public double getProgress() {
        return progress;
    }

    private static void writeInt(FileOutputStream fs, int size) throws IOException {
        BinaryBuffer buffer = new BinaryBuffer(4);
        buffer.append(size);

        fs.write(buffer.getBuffer());
    }
}