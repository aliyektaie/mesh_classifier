package edu.goergetown.bioasq.ui;

import edu.goergetown.bioasq.core.task.ITask;
import edu.goergetown.bioasq.tasks.preprocess.*;
import edu.goergetown.bioasq.tasks.train.*;
import edu.goergetown.bioasq.utils.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * Created by yektaie on 4/18/17.
 */
public class MainWindow implements ITaskListener {
    private final static double COEF = 2.2;
    private boolean enableParameterCallback = false;

    private void initTasks() {
        tasks.add(new ExtractDatasetOnPaperYearTask());
        tasks.add(new CreateDatasetMergeTask());
        tasks.add(new PartOfSpeechTaggerTask());
        tasks.add(new DocumentFeatureExtractorTask());
        tasks.add(new CreateMeSHProbabilityDistributionTask());
        tasks.add(new ClusterMeSHesTask());
        tasks.add(new HierarchicalAgglomerativeClusteringTask());
        tasks.add(new AdaptiveMeSHListClustererTask());
    }

    private static final Font FONT = new Font("Nato Sans 10", Font.PLAIN, c(14));
    public JPanel mainPanel;

    private ArrayList<ITask> tasks = new ArrayList<>();

    private JLabel lblSelectTask = new JLabel();
    private JLabel lblSelectTaskParameters = new JLabel();
    private JLabel lblProgress = new JLabel();
    private JComboBox cmbTasks = new JComboBox();
    private JComboBox cmbTaskParameters = new JComboBox();
    private JButton cmdRun = new JButton();
    private JTextArea txtLog = new JTextArea();
    private JScrollPane logContainer = null;
    private JProgressBar progress = new JProgressBar();

    private String taskCurrentState = "";
    private long taskStartTime = 0;
    private double taskProgress = 0;

    public MainWindow() {
        initTasks();

        mainPanel = new JPanel();
        mainPanel.setLayout(null);

        lblSelectTask.setText("Select Task:");
        lblSelectTask.setSize(c(200), c(25));
        lblSelectTask.setLocation(c(15),c(15));
        lblSelectTask.setFont(FONT);

        lblSelectTaskParameters.setText("Select Task Parameter:");
        lblSelectTaskParameters.setSize(c(200), c(25));
        lblSelectTaskParameters.setLocation(c(15),c(55));
        lblSelectTaskParameters.setFont(FONT);

        cmbTasks.setSize(c(804), c(25));
        cmbTasks.setLocation(c(200),c(12));
        cmbTasks.setFont(FONT);

        cmbTasks.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ITask task = (ITask) cmbTasks.getSelectedItem();
                cmbTaskParameters.removeAllItems();
                cmbTaskParameters.setEnabled(task.getParameters() != null);

                if (task.getParameters() != null) {
                    String key = (String) task.getParameters().keySet().toArray()[0];
                    ArrayList<Object> values = task.getParameters().get(key);
                    cmbTaskParameters.setName(key);

                    enableParameterCallback = false;
                    for (Object value : values) {
                        cmbTaskParameters.addItem(value);
                    }
                    enableParameterCallback = true;

                    cmbTaskParameters.setSelectedItem(task.getParameter(key));
                }
            }
        });

        cmbTaskParameters.setSize(c(804), c(25));
        cmbTaskParameters.setLocation(c(200),c(52));
        cmbTaskParameters.setFont(FONT);

        cmbTaskParameters.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (enableParameterCallback) {
                    ITask task = (ITask) cmbTasks.getSelectedItem();
                    task.setParameter(cmbTaskParameters.getName(), cmbTaskParameters.getSelectedItem());
                }
            }
        });

        for (ITask task : tasks) {
            cmbTasks.addItem(task);
        }

        cmbTasks.setSelectedIndex(cmbTasks.getItemCount() - 1);

        logContainer = new JScrollPane(txtLog);
        logContainer.setSize(c(1024 - 35), c(768 - 4*15 - 20 - 25 - 35 - 45));
        logContainer.setLocation(c(15),c(90));
        txtLog.setFont(new Font("Courier 10 Pitch", Font.PLAIN, c(15)));


        cmdRun.setText("Run Task");
        cmdRun.setSize(c(200), c(35));
        cmdRun.setLocation(c(1024 - 200 -   20),c(768 - 35 - 15 - 20));
        cmdRun.setFont(FONT);

        progress.setSize(c(1024 - 35), c(15));
        progress.setLocation(c(15),c(768 - 25 - 20));

        lblProgress.setText("Current State");
        lblProgress.setSize(c(1024 - 35), c(25));
        lblProgress.setLocation(c(15),c(768 - 35 - 33));
        lblProgress.setFont(FONT);

        mainPanel.add(lblSelectTask);
        mainPanel.add(lblSelectTaskParameters);
        mainPanel.add(cmbTasks);
        mainPanel.add(cmbTaskParameters);
        mainPanel.add(cmdRun);
        mainPanel.add(logContainer);
        mainPanel.add(progress);
        mainPanel.add(lblProgress);

        cmdRun.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onTaskSelected();
            }
        });

        showSelectMode();

    }

    private void onTaskSelected() {
        showRunMode();
        final boolean[] isStillRunning = {true};
        taskStartTime = System.currentTimeMillis();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ITask task = (ITask) cmbTasks.getSelectedItem();

                task.process(MainWindow.this);
                isStillRunning[0] = false;

                showSelectMode();

                log("\r\nFinished!");
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isStillRunning[0]) {
                    lblProgress.setText(String.format("%s %s %s", getProgress(),taskCurrentState,getStateAdditionalInfo()));
                    waitFor(200);
                }
            }
        }).start();
    }

    private String getProgress() {
        return "%" + String.format("%.3f", taskProgress);
    }

    private String getStateAdditionalInfo() {
        long elapsed = System.currentTimeMillis() - taskStartTime;
        String result = String.format("[%s / remained: %s]", formatTime((int) elapsed), formatTime(getRemainedTime(elapsed, taskProgress)));
        return result;
    }

    private int getRemainedTime(long elapsed, double progress) {
        if (progress == 0 || progress == 100 || elapsed == 0)
            return 0;

        return  (int)(elapsed * (100.0 - progress) / progress);
    }

    private String formatTime(int time) {
        time = time / 1000;
        int sec = time % 60;
        time = time / 60;
        int min = time % 60;
        time = time / 60;
        int hour = time;

        return String.format("%d:%s:%s", hour, pad(min), pad(sec));
    }

    private String pad(int value) {
        String result = value + "";

        if (result.length() == 1)
            result = "0" + result;

        return result;
    }

    private void waitFor(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    private void showRunMode() {
        cmbTasks.setEnabled(false);
        cmbTaskParameters.setEnabled(false);
        cmdRun.setVisible(false);

        progress.setVisible(true);
        lblProgress.setVisible(true);
    }

    private void showSelectMode() {
        cmbTasks.setEnabled(true);
        cmbTaskParameters.setEnabled(true);
        cmdRun.setVisible(true);

        progress.setVisible(false);
        lblProgress.setVisible(false);
    }

    @Override
    public void setCurrentState(String state) {
        taskCurrentState = state;
    }

    @Override
    public void setProgress(int value, int max) {
        progress.setMaximum(max);
        progress.setValue(value);

        taskProgress = value * 100.0 / max;
    }

    @Override
    public void log(String entry) {
        String time = formatTime((int) (System.currentTimeMillis() - taskStartTime));
        txtLog.append("[" + time + "] " + entry + "\r\n");


        JScrollBar vertical = logContainer.getVerticalScrollBar();
        vertical.setValue( vertical.getMaximum() );
    }

    @Override
    public void saveLogs(String path) {
        FileUtils.writeText(path, txtLog.getText());
    }

    public static int c(int value) {
        return (int) (value * COEF);
    }
}
