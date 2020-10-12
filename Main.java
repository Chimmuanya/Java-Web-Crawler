package crawler;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Main {
    static String getTitle(String text) {
        Pattern pattern = Pattern.compile("<title>.*?</title>");
        Matcher matcher = pattern.matcher(text);
        String siteTitle = "";

        if (matcher.find()) {
            siteTitle = matcher.group().replaceAll("</?title>", "").trim();
        }

        return siteTitle;

    }

  /*  static String getSiteText(String url) throws IOException {
        URLConnection myConnection = new URL(url).openConnection();
        myConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
        String siteText = "";
        if (myConnection.getContentType() != null && myConnection.getContentType().startsWith("text/html")) {
            try (InputStream inputStream = new BufferedInputStream(myConnection.getInputStream())) {
                siteText = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);


            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return siteText;
        } else {
            return null;
        }

    }

    static String getShortText(String url) throws IOException {
        URLConnection myConnection = new URL(url).openConnection();
        myConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
        String siteText = "";
        if (myConnection.getContentType() != null && myConnection.getContentType().startsWith("text/html")) {
            try (InputStream inputStream = new BufferedInputStream(myConnection.getInputStream())) {
                siteText = new String(inputStream.readNBytes(1024), StandardCharsets.UTF_8);


            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return siteText;
        } else {
            return null;
        }
    } */



    static Set<String> grabHtmlLinks(String text, String abs)throws IOException {
        Set<String> htmlLinks = new HashSet<>();
        String regex1 = "<a\\s+(?:[^>]*?\\s+)?href=([\"'])(.*?)\\1";
        Pattern pattern1 = Pattern.compile(regex1);
        Matcher matcher1 = pattern1.matcher(text);
        String myUrl = "";
        String url;
        htmlLinks.add(abs);
        while (matcher1.find()) {
            myUrl = matcher1.group(2);
            url = concatenateURL(myUrl, abs);
            //urls.add(myUrl);
            try {
                htmlLinks.add(url);
            } catch (NullPointerException n) {
                continue;
            }
        }
        return htmlLinks;
    }

    /* static String checkLinksTitles(String url) throws IOException {
        String title = "";
        String siteText = getShortText(url);
        if (siteText != null) {
            title = getTitle(siteText);
            return title;
        }
        return null;
    }


  static void saveData(Map<String, String> map, String address) throws IOException {
        BufferedWriter bufferedWriter = null;
        try {
            File file = new File(address);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(entry.getKey());
                sb.append("\n");
                sb.append(entry.getValue());
                sb.append("\n");
            }
            bufferedWriter.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (Exception e) {
                System.out.println("Error in closing the buffered writer" + e);
            }
        }




    } */


    private static String concatenateURL(String url, String absUrl) {
        String processedUrl = "";
        String adRegex = ".*?\\..*?\\..*?";
        String regex = "https?://.*";
        if (!url.matches(regex)) {
            if (url.contains("//")) {
                processedUrl = "https:" + url;

            } else if (url.matches(adRegex)) {
                processedUrl = "https://" + url;

            } else if (!url.startsWith("/") && !absUrl.endsWith("/")) {
                processedUrl = absUrl + "/" + url;

            } else if (url.startsWith("/") && absUrl.endsWith("/")) {
                processedUrl = absUrl + url.substring(1);
            }
            else {
                processedUrl = absUrl + url;
            }


        } else {
            processedUrl = url;
        }
        return processedUrl;
    }

}

class PageProcessor {
    private String baseUrl;
    private Set<String> urlsToCrawl = new HashSet<>();
    private String title = "";
    private volatile static int depthCounter = 0;
    public PageProcessor(String baseUrl) throws IOException {
        this.baseUrl = baseUrl;
        this.processPage();
    }

    public Set<String> getUrlsToCrawl() {
        return urlsToCrawl;
    }

    public String getTitle () {
        return title;
    }
    protected static int getDepthCounter () {
        return depthCounter;
    }

    protected static void resetDepthCounter() {
        depthCounter = 0;

    }

    private void processPage() throws IOException {
        String line;

        int counter = 0;

        URLConnection myConnection = new URL(baseUrl).openConnection();
        myConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
        if (myConnection.getContentType() != null && myConnection.getContentType().startsWith("text/html")) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(myConnection.getInputStream()))) {
                while ((line = br.readLine()) != null) {
                    counter++;
                    if (counter <= 20 && title.isEmpty()) {
                        title = Main.getTitle(line);
                    }
                    urlsToCrawl.addAll(Main.grabHtmlLinks(line, baseUrl));

                }
                depthCounter++;
            } catch (FileNotFoundException notFoundException){
                System.out.println("File not found at: " + baseUrl);
            }
        }

    }
}
