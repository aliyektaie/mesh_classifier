package edu.goergetown.bioasq.ui;

import edu.goergetown.bioasq.core.task.ITask;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * Created by Yektaie on 5/26/2017.
 */
public class ExplorerWindow {
    private final static double COEF = 2.2;

    private JPanel optionsPanel = new JPanel();
    private JPanel explorerPanel = new JPanel();

    private void initTasks() {
    }

    private static final Font FONT = new Font("Nato Sans 10", Font.PLAIN, c(14));
    public JPanel mainPanel;

    public ExplorerWindow() {
        initTasks();

        mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.setBackground(Color.WHITE);

        optionsPanel.setBackground(Color.GRAY);
        explorerPanel.setBackground(Color.WHITE);


        mainPanel.add(optionsPanel);
        mainPanel.add(explorerPanel);
    }

    public static int c(int value) {
        return (int) (value * COEF);
    }
}
