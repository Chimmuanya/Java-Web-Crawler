package crawler;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class WebCrawler extends JFrame {
    private static volatile long SEARCH_TIME;
    private Set<String> urlsToCrawl = new ConcurrentSkipListSet<>();
    //private ConcurrentMap<String, Boolean> visitedPages = new ConcurrentHashMap<>();
    private static Queue<String> tasksQueue = new ConcurrentLinkedQueue<>();
    private ConcurrentMap<String, String> finalData;
    private String firstUrl;
    private volatile int depth = 0;
    private volatile boolean isStopped = false;
    private volatile boolean isCounting = false;
    JLabel elapsedTime = new JLabel("0:00:00");
    JTextField depthTextField = new JTextField();
    JTextField workerTextField = new JTextField();
    volatile JTextField timeTextField = new JTextField();
    volatile JCheckBox depthCheckBox = new JCheckBox();
    volatile JCheckBox timeLimitCheckBox = new JCheckBox();
    private volatile ConcurrentMap<String, Boolean> visitedLinks;

    private volatile ExecutorService executor;
    private volatile ExecutorService executor1;
    JLabel parsedPagesLabel = new JLabel("0");
    private volatile long timeInSeconds = 0;
    volatile JToggleButton runButton = new JToggleButton("Run");
    volatile boolean isSaved = false;
    //volatile List<Future<?>> futures = new ArrayList<>();
    //volatile boolean isAllDone = false;
    //volatile boolean allDone = true;

    public WebCrawler() throws IOException {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        setSize(500, 400);
        setTitle("WebCrawler");


        GridBagConstraints mainConstraints = new GridBagConstraints();

        /////first column///////////////////////////

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        mainConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        mainConstraints.fill = GridBagConstraints.NONE;
        mainConstraints.weightx = 0;
        mainConstraints.weighty = 1;
        mainConstraints.insets = new Insets(5, 10, 0, 5);

        JLabel urlLabelText = new JLabel("Start URL: ");
        urlLabelText.setVisible(true);
        add(urlLabelText, mainConstraints);

        mainConstraints.gridy = 1;
        JLabel workersLabel = new JLabel("Workers: ");
        workersLabel.setVisible(true);
        add(workersLabel, mainConstraints);

        mainConstraints.gridy = 2;
        JLabel maxDepthLabel = new JLabel("Maximum depth: ");
        maxDepthLabel.setVisible(true);
        add(maxDepthLabel, mainConstraints);

        mainConstraints.gridy = 3;
        JLabel timeLimitLabel = new JLabel("Time limit: ");
        timeLimitLabel.setVisible(true);
        add(timeLimitLabel, mainConstraints);

        mainConstraints.gridy = 4;
        JLabel elapsedTimeLabel = new JLabel("Elapsed time: ");
        add(elapsedTimeLabel, mainConstraints);

        mainConstraints.gridy = 5;
        JLabel parsedLabel = new JLabel("Parsed pages: ");
        parsedLabel.setName("ParsedLabel");
        parsedLabel.setVisible(true);
        add(parsedLabel, mainConstraints);

        mainConstraints.gridy = 6;
        JLabel exportLabel = new JLabel("Export: ");
        exportLabel.setVisible(true);
        add(exportLabel, mainConstraints);

////////second column/////////////
        mainConstraints.gridx = 1;
        mainConstraints.gridy = 0;
        mainConstraints.gridwidth = GridBagConstraints.RELATIVE;
        mainConstraints.weightx = 0;
        mainConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainConstraints.anchor = GridBagConstraints.PAGE_START;
        mainConstraints.insets = new Insets(5, 0, 0, 10);

        JTextField urlTextField = new JTextField("");
        urlTextField.setName("UrlTextField");
        urlTextField.setVisible(true);
        add(urlTextField, mainConstraints);

        mainConstraints.gridx = 2;
        mainConstraints.insets = new Insets(5, 10, 0, 0);
        mainConstraints.gridwidth = GridBagConstraints.REMAINDER;
        mainConstraints.weightx = 0;
        mainConstraints.fill = GridBagConstraints.NONE;
        mainConstraints.anchor = GridBagConstraints.FIRST_LINE_END;


        runButton.addItemListener(itemEvent -> {
            if (runButton.isSelected()) {
                urlsToCrawl = new ConcurrentSkipListSet<>();
                visitedLinks = new ConcurrentHashMap<>();
                finalData = new ConcurrentHashMap<>();
                firstUrl = urlTextField.getText();
                if (depthTextField.getText().isEmpty()) {
                    depth = 0;
                    depthCheckBox.setEnabled(false);
                } else {
                    depth = Integer.parseInt(depthTextField.getText());
                }
                if (timeTextField.getText().isEmpty()) {
                    timeInSeconds = 0;
                    timeLimitCheckBox.setEnabled(false);
                } else {
                    SEARCH_TIME = Integer.parseInt(timeTextField.getText());
                }

                isCounting = false;
                isStopped = false;
                isSaved = false;
                depthCheckBox.setEnabled(false);
                timeLimitCheckBox.setEnabled(false);

                urlsToCrawl.add(firstUrl);
                startTime();
                if (workerTextField.getText().isEmpty()) {
                    start(3);
                } else {
                    start(Integer.parseInt(workerTextField.getText()));
                }
                parsedPagesCounter();

            } else if (!runButton.isSelected()){
                stop();
                depthCheckBox.setEnabled(true);
                timeLimitCheckBox.setEnabled(true);
                urlsToCrawl.clear();
                if (isSaved) {
                    finalData.clear();
                }
                isCounting = true;
                isStopped = true;
                timeInSeconds = 0;
                PageProcessor.resetDepthCounter();

            }
        });
        runButton.setName("RunButton");
        runButton.setVisible(true);
        add(runButton, mainConstraints);
        // addAction

        mainConstraints.gridx = 1;
        mainConstraints.gridy = 1;
        mainConstraints.weightx = 0.6;
        mainConstraints.insets = new Insets(0, 0, 0, 0);
        mainConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(workerTextField, mainConstraints);

        mainConstraints.gridy = 2;
        mainConstraints.weightx = 0.8;
        mainConstraints.gridwidth = 1;
        mainConstraints.insets = new Insets(0, 0, 0, 0);
        depthTextField.setName("DepthTextField");
        add(depthTextField, mainConstraints);

        mainConstraints.gridx = 2;
        mainConstraints.weightx = 0;
        mainConstraints.gridwidth = 1;
        mainConstraints.insets = new Insets(0, 0, 0, 0);
        mainConstraints.fill = GridBagConstraints.HORIZONTAL;
        depthCheckBox.setName("DepthCheckBox");
        add(depthCheckBox, mainConstraints);

        mainConstraints.gridx = 3;
        mainConstraints.fill = GridBagConstraints.NONE;
        mainConstraints.weightx = 0;
        mainConstraints.gridwidth = 1;
        mainConstraints.insets = new Insets(0, 0, 0, 0);
        JLabel enabledLabel = new JLabel("Enabled");
        add(enabledLabel, mainConstraints);

        mainConstraints.gridy = 3;
        mainConstraints.gridx = 1;
        mainConstraints.weightx = 0.4;
        mainConstraints.insets = new Insets(0, 0, 0, 5);
        mainConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(timeTextField, mainConstraints);

        mainConstraints.gridx = 2;
        mainConstraints.weightx = 0;
        JLabel secondsLabel = new JLabel("seconds");
        mainConstraints.fill = GridBagConstraints.NONE;
        add(secondsLabel, mainConstraints);

        mainConstraints.gridx = 3;
        mainConstraints.weightx = 0;


        mainConstraints.fill = GridBagConstraints.NONE;
        add(timeLimitCheckBox, mainConstraints);

        mainConstraints.gridx = 4;
        mainConstraints.weightx = 0;
        JLabel timeLimitEnabledLabel = new JLabel("Enabled");
        mainConstraints.fill = GridBagConstraints.NONE;
        add(timeLimitEnabledLabel, mainConstraints);

        mainConstraints.gridy = 4;
        mainConstraints.gridx = 1;
        mainConstraints.weightx = 0.8;
        add(elapsedTime, mainConstraints);

        mainConstraints.gridy = 5;
        mainConstraints.weightx = 0;
        add(parsedPagesLabel, mainConstraints);

        mainConstraints.gridy = 6;
        mainConstraints.weightx = 0.6;
        mainConstraints.gridwidth = 1;
        mainConstraints.fill = GridBagConstraints.HORIZONTAL;
        JTextField exportUrlTextField = new JTextField();
        exportUrlTextField.setName("ExportUrlTextField");
        add(exportUrlTextField, mainConstraints);

        mainConstraints.gridx = 2;
        mainConstraints.weightx = 0;
        mainConstraints.fill = GridBagConstraints.NONE;
        mainConstraints.insets = new Insets(0, 4, 0, 0);
        JButton saveButton = new JButton("Save");
        saveButton.setName("ExportButton");
        saveButton.addActionListener(l -> {
            try {
                saveData(exportUrlTextField.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        add(saveButton, mainConstraints);





    }



    private void saveData(String address) throws IOException {
        BufferedWriter bufferedWriter = null;
        try {
            File file = new File(address);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            StringBuilder sb = new StringBuilder();
            for (ConcurrentMap.Entry<String, String> entry : finalData.entrySet()) {
                sb.append(entry.getKey());
                sb.append("\n");
                sb.append(entry.getValue());
                sb.append("\n");
            }
            sb.deleteCharAt(sb.length() -1);
            bufferedWriter.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isStopped) {
                isSaved = true;
            }
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (Exception e) {
                System.out.println("Error in closing the buffered writer" + e);
            }
        }
    }

    private synchronized void start(int workerThreads) {

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {

                executor = Executors.newFixedThreadPool(workerThreads);
                executor1 = Executors.newFixedThreadPool(1);
                executor1.submit(() -> {
                    autoSet();
                });
                /*executor1.submit(() -> {
                    allDone = true;
                    try {
                        TimeUnit.SECONDS.sleep();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (Future<?> future : futures) {
                        allDone &= !future.isDone();
                    }
                    isAllDone = allDone;
                });*/
                for (String url : urlsToCrawl) {
                    submitCrawlTask(url);
                    urlsToCrawl.remove(url);
                    if (isStopped) {
                        
                        break;
                    }



                }



                return null;
            }




        };
        worker.execute();

    }

    private synchronized void stop() {
        try {
            executor.shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    private void autoSet() {
        while (runButton.isSelected()){
            if (depthCheckBox.isSelected()) {
                if (PageProcessor.getDepthCounter() == depth) {
                    runButton.doClick();
                }

            }
            if (timeLimitCheckBox.isSelected()) {
                if (timeInSeconds == SEARCH_TIME) {
                    runButton.doClick();
                }
            }
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (urlsToCrawl.isEmpty()) {

                runButton.doClick();
            }


        }
    }

    private void parsedPagesCounter() {

        SwingWorker<Void, String> swingWorker = new SwingWorker<>() {
            private int parsedPagesCount = 0;
            String parsedCount = "";

            @Override
            protected Void doInBackground() {
                while (true) {
                    if (isStopped) {
                        break;
                    }
                    parsedPagesCount = finalData.size();
                    parsedCount = String.valueOf(parsedPagesCount);
                    publish(parsedCount);

                }
                return null;
            }

            protected void process(List<String> chunks) {
                String mostRecent = chunks.get(chunks.size() - 1);
                parsedPagesLabel.setText(mostRecent);

            }


        };
        swingWorker.execute();
    }

    private void startTime() {

        SwingWorker<Void, String> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws InterruptedException {

                int sec = 0;
                int min = 0;
                int hr = 0;
                String timeElapsed;


                while (true) {
                    if (isCounting) {
                        break;
                    }
                    TimeUnit.SECONDS.sleep(1);
                    sec++;
                    timeInSeconds++;
                    if (sec == 60) {
                        min++;
                        sec = 0;
                    }
                    if (min == 60) {
                        hr++;
                        min = 0;
                    }

                    timeElapsed = String.format("%d:%02d:%02d", hr, min, sec);
                    publish(timeElapsed);


                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                String mostRecent = chunks.get(chunks.size() - 1);
                elapsedTime.setText(mostRecent);

            }

            protected void done() {
                elapsedTime.setText("0:00:00");
            }

        };


        worker.execute();



    }

    private void submitCrawlTask(String url) {
       /* try {
            Future<?> f = executor.submit(new CrawlTask(url));
            futures.add(f);

        } catch (Exception e) {

        }*/
        executor.submit(new CrawlTask(url));
    }

    private class CrawlTask implements Runnable {

    private final String url;
    private String title;
    protected CrawlTask(String url) {
        this.url = url;
    }
    boolean alreadyCrawled() {
        return visitedLinks.putIfAbsent(url, true) != null;
    }
        @Override
        public void run() {
            if (alreadyCrawled()) {
                return;
            }
            try {
                PageProcessor processor = new PageProcessor(url);
                Set<String> list = processor.getUrlsToCrawl();
                title = processor.getTitle();
                if (!title.isEmpty()) {
                    finalData.put(url, title);
                }
                for (String link: list) {
                    submitCrawlTask(link);
                    urlsToCrawl.add(link);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}



