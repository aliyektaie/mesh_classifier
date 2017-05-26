import javax.swing.*;

import edu.goergetown.bioasq.Constants;
import edu.goergetown.bioasq.core.model.Cluster;
import edu.goergetown.bioasq.ui.ExplorerWindow;
import edu.goergetown.bioasq.ui.MainWindow;
import edu.goergetown.bioasq.utils.FileUtils;

import java.util.ArrayList;

/**
 * Created by Yektaie on 5/10/2017.
 */
public class Program {
    public static void main(String[] args) {
        Constants.initialize();

        setupLookAndFeel();
        JFrame window = new JFrame("Health Search & Mining Project");

        if (isExplorerMode(args)) {
            window.setContentPane(new ExplorerWindow().mainPanel);
            window.setSize(MainWindow.c(1500), MainWindow.c(900));
        } else {
            window.setContentPane(new MainWindow().mainPanel);
            window.setSize(MainWindow.c(1034), MainWindow.c(788));
        }

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.doLayout();
        window.setVisible(true);
    }

    private static boolean isExplorerMode(String[] args) {
        if (args.length == 1 && args[0].equals("--explorer")) {
            return true;
        }

        return false;
    }

    private static void setupLookAndFeel() {
        try {
            // Set cross-platform Java L&F (also called "Metal")
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            // handle exception
        }
    }

    private static void mergeGraphDistributions() {
        double[][] values = new double[101][];
        double[] max= new double[Constants.CORE_COUNT];
        double[] sums= new double[Constants.CORE_COUNT];
        for (int i = 0; i < values.length; i++) {
            values[i] = new double[Constants.CORE_COUNT];
        }

        for (int i = 0; i < Constants.CORE_COUNT; i++) {
            String[] lines = FileUtils.readAllLines("E:\\data\\Temp\\graph-weights-distribution-" + i + ".csv");

            for (int j = 0; j < values.length; j++) {
                String[] parts = lines[j].split(",");
                double value = Double.valueOf(parts[1]);
                if ( j > 0) {
                    values[j][i] = value;
                    if (max[i] < value) {
                        max[i] = value;
                    }
                    sums[i] += value;
                }
            }
        }

        StringBuilder result = new StringBuilder();
        double sumMax = 0;
        for (int i = 0; i < max.length; i++) {
            sumMax += max[i];
        }

        String nl = "";
        double sum = 0;
        for (int i = 0; i < 101; i++) {
            result.append(nl);
            nl = "\r\n";

            result.append(String.format("%.2f", i * 0.01)).append(",");
            for (int j = 0; j < Constants.CORE_COUNT; j++) {
                double value = values[i][j] / sums[j];
                sum += value;
                result.append(String.format("%.5f", value)).append(",");
            }
            result.append(String.format("%.5f", sum / Constants.CORE_COUNT));
        }

        FileUtils.writeText("E:\\data\\Temp\\graph-weights-distribution-accumulative.csv", result.toString());
    }
}
