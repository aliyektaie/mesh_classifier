package edu.goergetown.bioasq.tasks.evaluation;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.model.classification.ClassifierParameter;
import edu.goergetown.bioasq.core.model.classification.MeSHClassificationModelBase;
import edu.goergetown.bioasq.core.task.BaseTask;
import edu.goergetown.bioasq.core.task.SubTaskInfo;
import edu.goergetown.bioasq.ui.ITaskListener;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Yektaie on 5/29/2017.
 */
public class CreateClassifierModelFileTask extends BaseTask {
    private SubTaskInfo CREATE_MODEL_TASK = new SubTaskInfo("Create classifier model", 10);

    private ClassifierParameter parameter = null;
    private ArrayList<ClassifierParameter> configurations = null;

    public CreateClassifierModelFileTask() {
        configurations = ClassifierParameter.getConfigurations();
        parameter = configurations.get(0);
    }

    @Override
    public void process(ITaskListener listener) {
        if (!MeSHClassificationModelBase.exists(parameter)) {
            MeSHClassificationModelBase.create(listener, CREATE_MODEL_TASK, parameter);
        }
    }

    @Override
    protected String getTaskClass() {
        return CLASS_TRAIN;
    }

    @Override
    protected String getTaskTitle() {
        return "Create Classifier Model File";
    }

    @Override
    protected ArrayList<SubTaskInfo> getSubTasks() {
        ArrayList<SubTaskInfo> result = new ArrayList<>();

        result.add(CREATE_MODEL_TASK);

        return result;
    }

    @Override
    public Object getParameter(String name) {
        return parameter;
    }

    @Override
    public void setParameter(String name, Object value) {
        if (value != null) {
            parameter = (ClassifierParameter) value;
        }
    }

    @Override
    public Hashtable<String, ArrayList<Object>> getParameters() {
        Hashtable<String, ArrayList<Object>> result = new Hashtable<>();
        ArrayList<Object> configs = new ArrayList<>();
        configs.addAll(configurations);

        result.put("configurations", configs);

        return result;
    }
}

