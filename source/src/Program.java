import edu.goergetown.bioasq.ui.MainWindow;

import javax.swing.*;

/**
 * Created by Yektaie on 5/10/2017.
 */
public class Program {
    public static void main(String[] args) {
        setupLookAndFeel();

        JFrame window = new JFrame("Health Search & Mining Project");
        window.setContentPane(new MainWindow().mainPanel);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(MainWindow.c(1034), MainWindow.c(788));
        window.doLayout();
        window.setVisible(true);
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
}
