package com.wavjaby;

import com.sun.net.httpserver.HttpServer;
import io.github.sashirestela.openai.SimpleOpenAI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

public class Main {

    public static String apiKey;

    public Main() throws IOException {
        Properties settings = new Properties();
        Path target = Path.of("./settings.properties");
        if (!target.toFile().exists()) {
            System.out.println("Loading default settings file");
            Files.copy(requireNonNull(Main.class.getResourceAsStream("/settings.properties")), target);
            return;
        }
        settings.load(Files.newInputStream(target));
        apiKey = settings.getProperty("OPEN_AI_KEY");
        if (apiKey == null) {
            System.err.println("No OpenAI API key found");
            return;
        }

        File imageProcessingPrompt = new File(settings.getProperty("IMAGE_PROCESSING_PROMPT"));
        File problemSolvingPrompt = new File(settings.getProperty("PROBLEM_SOLVING_PROMPT"));
        File resultAnalysisPrompt = new File(settings.getProperty("RESULT_ANALYSIS_PROMPT"));
        if (!problemSolvingPrompt.exists() || !resultAnalysisPrompt.exists()) {
            System.err.println("Prompt file not found");
            return;
        }

        SimpleOpenAI openAI = SimpleOpenAI.builder()
                .apiKey(apiKey)
                .organizationId(settings.getProperty("ORGANIZATION_ID"))
                .projectId(settings.getProperty("PROJECT_ID"))
                .build();

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(25569), 0);
        httpServer.createContext("/img", new ImageProcessHandler(openAI, imageProcessingPrompt, problemSolvingPrompt, resultAnalysisPrompt));
        httpServer.createContext("/ota", new OTAHandler(settings.getProperty("OTA_FILE")));
        httpServer.setExecutor(Executors.newFixedThreadPool(10));

        httpServer.start();
        System.out.println("Server started at " + httpServer.getAddress());

//        File image = new File("./image.jpg");
//        // Post image to httpServer
//        HttpURLConnection connection = (HttpURLConnection) new URL("http://0.0.0.0:25569/img").openConnection();
//        connection.setRequestMethod("POST");
//        connection.setRequestProperty("Content-Type", "image/jpeg");
//        connection.setDoOutput(true);
//        Files.copy(image.toPath(), connection.getOutputStream());
//        // Read response
//        InputStream inputStream = connection.getInputStream();
//        byte[] buffer = new byte[inputStream.available()];
//        inputStream.read(buffer);
//        inputStream.close();
//        String requestId = new String(buffer);
//        System.out.println(requestId + ": Image processing");
//        // Wait response
//        while (true) {
//            connection = (HttpURLConnection) new URL("http://0.0.0.0:25569/img").openConnection();
//            connection.setRequestProperty("id", requestId);
//            connection.setRequestMethod("GET");
//            inputStream = connection.getInputStream();
//            inputStream.close();
//            if (connection.getResponseCode() == 102) {
//                System.out.println(requestId + ": Solution generating");
//            } else {
//                System.out.println(requestId + ": Done");
//                break;
//            }
//        }
    }

    public static void main(String[] args) throws IOException {
        new Main();
    }
}