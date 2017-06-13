package edu.goergetown.bioasq.utils;

import com.google.common.collect.Lists;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.kernel.LinearKernel;
import edu.berkeley.compbio.jlibsvm.multi.MultiClassModel;
import edu.berkeley.compbio.jlibsvm.scaler.LinearScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.scaler.ZscoreScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;
import edu.goergetown.bioasq.core.Guid;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by Yektaie on 6/6/2017.
 */
public class ObjectSerializer {
    private static ArrayList<String> primitives = new ArrayList<>();

    static {
        primitives.add("java.lang.Float");
        primitives.add("java.lang.Boolean");
        primitives.add("java.lang.Class");
        primitives.add("java.lang.Double");
        primitives.add("java.lang.Integer");
        primitives.add("java.lang.String");

    }

    public static byte[] serialize(BinaryModel model) {
        try {
            byte[] params = serializeParameters(model);
            byte[] svs = serializeSupportVectors(model);

            model.getClass().getField("SVs").set(model, null);
            model.getClass().getField("param").set(model, null);

            byte[] object = serializeObject(model);

            BinaryBuffer buffer = new BinaryBuffer(12 + params.length + svs.length + object.length);
            buffer.append(object);
            buffer.append(params);
            buffer.append(svs);

            return buffer.getBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static byte[] serializeObject(Object obj) {
        if (obj == null) {
            return nullObject();
        }

        Class modelClass = obj.getClass();
        if (primitives.contains(modelClass.getName()) || obj instanceof double[]) {
            return serializePrimitives(obj);
        }

        int length = 1;
        length += BinaryBuffer.getSerializedLength(modelClass.getName());

        length += 4;
        Field[] fields = getFields(modelClass);

        for (int i = 0; i < fields.length; i++) {
            try {
                Object value = fields[i].get(obj);
                byte[] valueSerialized = serializeObject(value);

                String name = fields[i].getName();
                length += BinaryBuffer.getSerializedLength(name);
                length += BinaryBuffer.getSerializedLength(valueSerialized);
            } catch (IllegalAccessException ignored) {
            }
        }

        BinaryBuffer buffer = new BinaryBuffer(length);

        buffer.append((byte) 1);
        buffer.append(modelClass.getName());

        buffer.append(fields.length);
        for (int i = 0; i < fields.length; i++) {
            try {
                Object value = fields[i].get(obj);
                byte[] valueSerialized = serializeObject(value);

                String name = fields[i].getName();
                buffer.append(name);
                buffer.append(valueSerialized);
            } catch (IllegalAccessException ignored) {
            }
        }

        return buffer.getBuffer();
    }

    private static byte[] objectFromIdentifier(ObjectIDPair pair) {
        int length = 1;
        length += BinaryBuffer.getSerializedLength(pair.id);

        BinaryBuffer buffer = new BinaryBuffer(length);

        buffer.append((byte) 2);
        buffer.append(pair.id);

        return buffer.getBuffer();
    }

    private static ObjectIDPair addObjectIdentifier(Object obj, Hashtable<Object, ObjectIDPair> objects) {
        ObjectIDPair pair = new ObjectIDPair();
        pair.id = Guid.NewGuid().toString();
        pair.object = obj;
        objects.put(obj, pair);

        return pair;
    }

    private static byte[] serializePrimitives(Object obj) {
        Class cls = obj.getClass();

        primitives.add("java.lang.Boolean");
        primitives.add("java.lang.Class");
        primitives.add("java.lang.Double");
        primitives.add("java.lang.Integer");
        primitives.add("java.lang.String");

        if (cls.getName().equals("java.lang.Float")) {
            int length = 1;
            length += BinaryBuffer.getSerializedLength(cls.getName());
            length += 4;

            BinaryBuffer buffer = new BinaryBuffer(length);
            buffer.append((byte) 1);
            buffer.append(cls.getName());
            buffer.append((float) obj);

            return buffer.getBuffer();
        } else if (cls.getName().equals("java.lang.Boolean")) {
            int length = 1;
            length += BinaryBuffer.getSerializedLength(cls.getName());
            length += 1;

            BinaryBuffer buffer = new BinaryBuffer(length);
            buffer.append((byte) 1);
            buffer.append(cls.getName());
            buffer.append((boolean) obj);

            return buffer.getBuffer();
        } else if (cls.getName().equals("java.lang.Class")) {
            int length = 1;
            length += BinaryBuffer.getSerializedLength(cls.getName());
            length += BinaryBuffer.getSerializedLength(((Class)obj).getName());

            BinaryBuffer buffer = new BinaryBuffer(length);
            buffer.append((byte) 1);
            buffer.append(cls.getName());
            buffer.append(((Class)obj).getName());

            return buffer.getBuffer();
        } else if (cls.getName().equals("java.lang.Double")) {
            int length = 1;
            length += BinaryBuffer.getSerializedLength(cls.getName());
            length += 4;

            BinaryBuffer buffer = new BinaryBuffer(length);
            buffer.append((byte) 1);
            buffer.append(cls.getName());
            double value = (double) obj;
            buffer.append((float) value);

            return buffer.getBuffer();
        } else if (cls.getName().equals("java.lang.Integer")) {
            int length = 1;
            length += BinaryBuffer.getSerializedLength(cls.getName());
            length += 4;

            BinaryBuffer buffer = new BinaryBuffer(length);
            buffer.append((byte) 1);
            buffer.append(cls.getName());
            buffer.append((int) obj);

            return buffer.getBuffer();
        } else if (cls.getName().equals("java.lang.String")) {
            int length = 1;
            length += BinaryBuffer.getSerializedLength(cls.getName());
            length += BinaryBuffer.getSerializedLength((String)obj);

            BinaryBuffer buffer = new BinaryBuffer(length);
            buffer.append((byte) 1);
            buffer.append(cls.getName());
            buffer.append((String) obj);

            return buffer.getBuffer();
        } else if (obj instanceof double[]) {
            int length = 1;
            double[] array = (double[]) obj;
            length += BinaryBuffer.getSerializedLength("double[]");
            int l = array.length * 4;
            length += l;
            length += 4;

            BinaryBuffer buffer = new BinaryBuffer(length);
            buffer.append((byte) 1);
            buffer.append("double[]");
            buffer.append(array.length);
            for (int i = 0; i < array.length; i++) {
                buffer.append((float) array[i]);
            }

            return buffer.getBuffer();
        }

        return new byte[0];
    }

    private static byte[] nullObject() {
        return new byte[1];
    }

    public static Object load(byte[] data) {
        try {
            BinaryBuffer buffer = new BinaryBuffer(data);
            byte[] object = buffer.readByteArray();
            byte[] params = buffer.readByteArray();
            byte[] svs = buffer.readByteArray();

            Object result =  loadObject(object);
            result.getClass().getField("param").set(result, loadParameters(params));
            result.getClass().getField("SVs").set(result, loadVectors(svs));

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object loadVectors(byte[] svs) {
        BinaryBuffer buffer= new BinaryBuffer(svs);
        int count = buffer.readInt();
        SparseVector[] result = new SparseVector[count];

        for (int i = 0; i < count; i++) {
            result[i] = SparseVector.load(buffer.readByteArray());
        }

        return result;
    }

    private static Object loadParameters(byte[] params) {
        ImmutableSvmParameterPoint result = new ImmutableSvmParameterPoint();
        BinaryBuffer buffer = new BinaryBuffer(params);

        result.C = buffer.readFloat();
        result.kernel = createKernel(buffer.readString());
        result.cache_size = buffer.readFloat();
        result.eps = buffer.readFloat();
        result.nu = buffer.readFloat();
        result.p = buffer.readFloat();
        result.shrinking = buffer.readBoolean();
        result.oneVsAllThreshold = buffer.readFloat();
        result.oneVsAllMode = toOneEnum(buffer.readString());
        result.allVsAllMode = toAllEnum(buffer.readString());
        result.minVoteProportion = buffer.readFloat();
        result.falseClassSVlimit = buffer.readInt();
        result.redistributeUnbalancedC = buffer.readBoolean();
        result.scaleBinaryMachinesIndependently = buffer.readBoolean();
        result.gridsearchBinaryMachinesIndependently = buffer.readBoolean();
        result.normalizeL2 = buffer.readBoolean();
        result.probability = buffer.readBoolean();
        result.crossValidationFolds = buffer.readInt();

        if (buffer.readBoolean()) {
            int type = buffer.readByte();
            byte[] d = buffer.readByteArray();

            result.scalingModelLearner = getScalingModelLearner(type, d);
        } else {
            result.scalingModelLearner = null;
        }

        int count = buffer.readInt();
        result.weights = new LinkedHashMap();
        for (int i = 0; i < count; i++) {
            String key = buffer.readString();
            float value = buffer.readFloat();

            result.weights.put(key, value);
        }

        return result;
    }

    private static KernelFunction createKernel(String s) {
        if (s.equals("linear")) {
            return new LinearKernel();
        }

        return null;
    }


    private static MultiClassModel.AllVsAllMode toAllEnum(String s) {
        switch (s) {
            case "None":
                return MultiClassModel.AllVsAllMode.None;
            case "AllVsAll":
                return MultiClassModel.AllVsAllMode.AllVsAll;
            case "FilteredVsAll":
                return MultiClassModel.AllVsAllMode.FilteredVsAll;
            case "FilteredVsFiltered":
                return MultiClassModel.AllVsAllMode.FilteredVsFiltered;
        }

        return MultiClassModel.AllVsAllMode.None;
    }

    private static MultiClassModel.OneVsAllMode toOneEnum(String s) {
        switch (s) {
            case "None":
                return MultiClassModel.OneVsAllMode.None;
            case "Best":
                return MultiClassModel.OneVsAllMode.Best;
            case "Veto":
                return MultiClassModel.OneVsAllMode.Veto;
            case "BreakTies":
                return MultiClassModel.OneVsAllMode.BreakTies;
            case "VetoAndBreakTies":
                return MultiClassModel.OneVsAllMode.VetoAndBreakTies;
        }

        return MultiClassModel.OneVsAllMode.None;
    }

    private static Object loadObject(byte[] data) throws Exception {
        BinaryBuffer buffer = new BinaryBuffer(data);
        int type = buffer.readByte();

        if (type == 0) {
            return null;
        } else if (type == 1) {
            String className = buffer.readString();

            if (primitives.contains(className) || className.equals("double[]")) {
                return loadPrimitive(buffer, className);
            } else {
                Object result = Class.forName(className).newInstance();
                Class resultClass = result.getClass();

                int fieldCount = buffer.readInt();
                for (int i = 0; i < fieldCount; i++) {
                    String name = buffer.readString();
                    byte[] fieldData = buffer.readByteArray();

                    Object value = loadObject(fieldData);
                    resultClass.getField(name).set(result, value);
                }

                return result;
            }
        }

        return null;
    }

    private static Object loadPrimitive(BinaryBuffer buffer, String className) throws ClassNotFoundException {
        if (className.equals("java.lang.Float")) {
            return buffer.readFloat();
        } else if (className.equals("java.lang.Boolean")) {
            return buffer.readBoolean();
        } else if (className.equals("java.lang.Class")) {
            String c = buffer.readString();
            return Class.forName(c);
        } else if (className.equals("java.lang.Double")) {
            return buffer.readFloat();
        } else if (className.equals("java.lang.Integer")) {
            return buffer.readInt();
        } else if (className.equals("java.lang.String")) {
            return buffer.readString();
        } else if (className.equals("double[]")) {
            int count = buffer.readInt();
            double[] result = new double[count];

            for (int i = 0; i < count; i++) {
                result[i] = buffer.readFloat();
            }

            return result;
        }

        return null;
    }

    private static byte[] serializeParameters(BinaryModel model) throws Exception {
        ImmutableSvmParameterPoint params = (ImmutableSvmParameterPoint) model.getClass().getField("param").get(model);

        float c = params.C;
        String kernelType = params.kernel.toString();
        kernelType = kernelType.substring(kernelType.indexOf(" ")).trim();

        float cachSize = params.cache_size;
        float eps = params.eps;
        float nu = params.nu;
        float p = params.p;
        boolean shrinking = params.shrinking;
        float oneVsAllThreshold = (float) params.oneVsAllThreshold;
        String oneVsAllMode = params.oneVsAllMode.name();
        String allVsAllMode = params.allVsAllMode.name();
        float minVoteProportion = (float) params.minVoteProportion;
        int falseClassSVlimit = params.falseClassSVlimit;
        boolean redistributeUnbalancedC = params.redistributeUnbalancedC;
        boolean scaleBinaryMachinesIndependently = params.scaleBinaryMachinesIndependently;
        boolean gridsearchBinaryMachinesIndependently = params.gridsearchBinaryMachinesIndependently;
        boolean normalizeL2 = params.normalizeL2;
        boolean probability = params.probability;
        int crossValidationFolds = params.crossValidationFolds;
        ScalingModelLearner scalingModelLearner = params.scalingModelLearner;
        LinkedHashMap weights = params.weights;

        int length = 0;
        length += (4 * 9);
        length += 6;
        length += BinaryBuffer.getSerializedLength(kernelType);
        length += BinaryBuffer.getSerializedLength(oneVsAllMode);
        length += BinaryBuffer.getSerializedLength(allVsAllMode);
        length += 1;
        if (scalingModelLearner != null) {
            length += 1;
            length += BinaryBuffer.getSerializedLength(scalingModelLearner.serialize());
        }

        length += 4;
        for (Object key : weights.keySet()) {
            length += BinaryBuffer.getSerializedLength(key.toString());
            length += 4;
        }

        BinaryBuffer buffer = new BinaryBuffer(length);

        buffer.append(c);
        buffer.append(kernelType);
        buffer.append(cachSize);
        buffer.append(eps);
        buffer.append(nu);
        buffer.append(p);
        buffer.append(shrinking);
        buffer.append(oneVsAllThreshold);
        buffer.append(oneVsAllMode);
        buffer.append(allVsAllMode);
        buffer.append(minVoteProportion);
        buffer.append(falseClassSVlimit);
        buffer.append(redistributeUnbalancedC);
        buffer.append(scaleBinaryMachinesIndependently);
        buffer.append(gridsearchBinaryMachinesIndependently);
        buffer.append(normalizeL2);
        buffer.append(probability);
        buffer.append(crossValidationFolds);

        if (scalingModelLearner == null) {
            buffer.append(false);
        } else {
            buffer.append(true);
            buffer.append((byte)getScalingModelLearnerType(scalingModelLearner));
            buffer.append(scalingModelLearner.serialize());
        }

        buffer.append(weights.size());
        for (Object key : weights.keySet()) {
            buffer.append(key.toString());
            buffer.append((float)weights.get(key));
        }

        return buffer.getBuffer();
    }

    private static int getScalingModelLearnerType(ScalingModelLearner learner) {
        int result = -1;

        if (learner instanceof NoopScalingModelLearner) {
            result  = 0;
        } else if (learner instanceof LinearScalingModelLearner) {
            result = 1;
        } else if (learner instanceof ZscoreScalingModelLearner) {
            result = 2;
        }

        return result;
    }

    private static ScalingModelLearner getScalingModelLearner(int type, byte[] d) {
        ScalingModelLearner result = null;

        if (type == 0) {
            result = new NoopScalingModelLearner();
        } else if (type == 1) {
            result = new LinearScalingModelLearner();
        } else if (type == 2) {
            result = new ZscoreScalingModelLearner();
        }

        if (result != null) {
            result.load(d);
        }

        return result;
    }


    private static byte[] serializeSupportVectors(BinaryModel obj) {
        Object[] vectors = obj.SVs;
        ArrayList<byte[]> datas = new ArrayList<>();

        for (int i = 0; i < vectors.length; i++) {
            datas.add(((SparseVector)vectors[i]).serialize());
        }

        int length = 0;
        length += 4;
        for (byte[] data : datas) {
            length += BinaryBuffer.getSerializedLength(data);
        }

        BinaryBuffer result = new BinaryBuffer(length);

        result.append(datas.size());
        for (int i = 0; i < datas.size(); i++) {
            result.append(datas.get(i));
        }

        return result.getBuffer();
    }

    private static Field[] getFields(Class<?> cls) {
        return cls.getFields();
//        ArrayList<Field> list = getFieldsUpTo(cls, Object.class);
//        Field[] result = new Field[list.size()];
//
//        for (int i = 0; i < list.size(); i++) {
//            result[i] = list.get(i);
//        }
//
//        return result;
    }

    private static ArrayList<Field> getFieldsUpTo(Class<?> startClass,
                                                  Class<?> exclusiveParent) {

        ArrayList<Field> currentClassFields = new ArrayList<>(Lists.newArrayList(startClass.getDeclaredFields()));
        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null && (exclusiveParent == null || !(parentClass.equals(exclusiveParent)))) {
            List<Field> parentClassFields = getFieldsUpTo(parentClass, exclusiveParent);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }
}

class ObjectIDPair {
    public Object object = null;
    public String id = "";
    public boolean added = false;
}