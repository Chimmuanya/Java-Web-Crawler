package crawler;

import javax.swing.*;
import java.io.IOException;


public class ApplicationRunner {

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, IOException {

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
       
        SwingUtilities.invokeLater(() -> {
            WebCrawler webCrawlerGUI = null;
            try {
                webCrawlerGUI = new WebCrawler();
            } catch (IOException e) {
                e.printStackTrace();
            }
            webCrawlerGUI.setVisible(true);
            webCrawlerGUI.setLocationRelativeTo(null);
        });

    }
}