package com.wavjaby;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.wavjaby.lib.Bitmap;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.common.content.ContentPart.ContentPartImageUrl;
import io.github.sashirestela.openai.common.content.ContentPart.ContentPartImageUrl.ImageUrl;
import io.github.sashirestela.openai.common.content.ImageDetail;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static com.wavjaby.lib.ImageProcess.*;

public class ImageProcessHandler implements HttpHandler {
    private final Map<String, CompletableFuture<Chat>> imageRequests = new HashMap<>();
    private final Map<String, List<CompletableFuture<Chat>>> solvingRequests = new HashMap<>();
    private final Map<String, CompletableFuture<Chat>> analyzeRequests = new HashMap<>();
    private final SimpleOpenAI openAI;
    private final File imageProcessingPromptFile, problemSolvingPromptFile, resultAnalysisPromptFile;
    private long imageProcessingPromptTime = -1, problemSolvingPromptTime = -1, resultAnalysisPromptTime = -1;
    private String imageProcessingPrompt, problemSolvingPrompt, resultAnalysisPrompt;

    public ImageProcessHandler(SimpleOpenAI openAI, File imageProcessingPromptFile, File problemSolvingPromptFile, File resultAnalysisPromptFile) {
        this.openAI = openAI;
        this.imageProcessingPromptFile = imageProcessingPromptFile;
        this.problemSolvingPromptFile = problemSolvingPromptFile;
        this.resultAnalysisPromptFile = resultAnalysisPromptFile;
        checkPromptFileUpdate();

//        try {
//            byte[] imageBytes= Files.readAllBytes(Path.of("C:\\Users\\user\\Downloads\\2ce60af4-f743-4ed7-8940-7d5a3bbffa0f.jpg"));
//            CompletableFuture<Chat> future0 = sendImage(imageBytes);
//            CompletableFuture<Chat> future1 = sendImage(imageBytes);
//            analyzeGptAnswer(List.of(future0, future1));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    private void checkPromptFileUpdate() {
        try {
            if (imageProcessingPromptFile.lastModified() != imageProcessingPromptTime) {
                imageProcessingPromptTime = imageProcessingPromptFile.lastModified();
                var lines = Files.readAllLines(imageProcessingPromptFile.toPath());
                imageProcessingPrompt = String.join("\n", lines);
            }
            if (problemSolvingPromptFile.lastModified() != problemSolvingPromptTime) {
                problemSolvingPromptTime = problemSolvingPromptFile.lastModified();
                var lines = Files.readAllLines(problemSolvingPromptFile.toPath());
                problemSolvingPrompt = String.join("\n", lines);
            }
            if (resultAnalysisPromptFile.lastModified() != resultAnalysisPromptTime) {
                resultAnalysisPromptTime = resultAnalysisPromptFile.lastModified();
                var lines = Files.readAllLines(resultAnalysisPromptFile.toPath());
                resultAnalysisPrompt = String.join("\n", lines);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //    private final String model = "gpt-4o-2024-11-20";
    private final String model = "o4-mini";
    private final String imageProcessModel = "gpt-4o-mini";

    private CompletableFuture<Chat> processImage(byte[] image) {
        checkPromptFileUpdate();

        ChatRequest.ChatRequestBuilder request = ChatRequest.builder()
                .model(imageProcessModel)
                .message(SystemMessage.of(imageProcessingPrompt))
                .temperature(0.0)
                .maxCompletionTokens(2048);

        // Payload
        request.message(UserMessage.of(List.of(
                contentPartImageBase64(image)
        )));

        return openAI.chatCompletions().create(request.build());
    }

    private CompletableFuture<Chat> solveProblem(String problem) {
        checkPromptFileUpdate();

        ChatRequest.ChatRequestBuilder request = ChatRequest.builder()
                .model(model)
                .message(SystemMessage.of(problemSolvingPrompt))
                .reasoningEffort(ChatRequest.ReasoningEffort.HIGH)
                .maxCompletionTokens(4096);

        // Payload
        request.message(UserMessage.of(problem));

//        time = System.currentTimeMillis();
//        CompletableFuture<Stream<Chat>> response = openAI.chatCompletions().createStream(request);
//        response.join().forEach(this::processResponseChunk);

        return openAI.chatCompletions().create(request.build());
    }

    private CompletableFuture<Chat> analysis(String solutions) {
        checkPromptFileUpdate();

        ChatRequest.ChatRequestBuilder request = ChatRequest.builder()
                .model(model)
                .message(SystemMessage.of(resultAnalysisPrompt))
                .reasoningEffort(ChatRequest.ReasoningEffort.MEDIUM)
                .store(true)
                .maxCompletionTokens(2048);

        // Payload
        request.message(UserMessage.of(solutions));

        return openAI.chatCompletions().create(request.build());
    }

    private void gptProcessImage(String uuid, byte[] imageBytes) {
        CompletableFuture<Chat> future = processImage(imageBytes);
        imageRequests.put(uuid, future);
    }

    private int gptSolveProblem(String uuid, Chat chat) {
        String problem = chat.firstMessage().getContent();
        System.out.println("##########RAW########## " + chat.getUsage().getTotalTokens() +
                           "\n" + problem +
                           "\n##########RAW##########");

        if (problem.startsWith("blurry"))
            return -1;

        CompletableFuture<Chat> future0 = solveProblem(problem);
        CompletableFuture<Chat> future1 = solveProblem(problem);
        List<CompletableFuture<Chat>> results = List.of(future0, future1);
        solvingRequests.put(uuid, results);
        return results.size();
    }

    private void gptAnalyzeAnswer(String requestId, List<CompletableFuture<Chat>> results) {
        int tokenUse = 0;
        StringBuilder resultString = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            resultString.append("# Solution ").append(i).append("\n");
            Chat response = results.get(i).join();
            tokenUse += response.getUsage().getTotalTokens();
            String msg = response.firstMessage().getContent();
            resultString.append(msg);
        }
        System.out.println("Token use: " + tokenUse);
        analyzeRequests.put(requestId, analysis(resultString.toString()));
    }

    private String gptGetFinalResult(Chat result) {
        int tokenUse = result.getUsage().getTotalTokens();
        String analyzeResult = result.firstMessage().getContent();

        System.out.println("##########RAW########## " + tokenUse +
                           "\n" + analyzeResult +
                           "\n##########RAW##########");
        return analyzeResult.replace('\n',' ');
    }

    private Object contentPartImageBase64(byte[] image) {
        return ContentPartImageUrl.of(ImageUrl.of(
                "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image), ImageDetail.HIGH));
    }

    private void receiveImageFromClient(HttpExchange exchange) throws IOException {
        // Read image
        BufferedImage image = ImageIO.read(exchange.getRequestBody());

        if (true) {
            image = scaleImageBaseOnWidth(1024, image);
            image = sharpenImage(0.5f, image);
        }

        byte[] imageBytes = createJpgDataBytes(0.9f, image);

        String uuid = UUID.randomUUID().toString();

        // Original image
        FileOutputStream imgDebugOut = new FileOutputStream("images/" + uuid + ".jpg");
        imgDebugOut.write(imageBytes, 0, imageBytes.length);
        imgDebugOut.close();

        gptProcessImage(uuid, imageBytes);

        // Return code
        byte[] data = uuid.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private void getResult(HttpExchange exchange) throws IOException {
        String requestId = exchange.getRequestHeaders().getFirst("id");
        if (imageRequests.containsKey(requestId)) {
            CompletableFuture<Chat> results = imageRequests.remove(requestId);
            Chat response = results.join();
            int processCount = gptSolveProblem(requestId, response);

            // Processing
            exchange.sendResponseHeaders(102, -1);
            exchange.close();
        } else if (solvingRequests.containsKey(requestId)) {
            List<CompletableFuture<Chat>> results = solvingRequests.remove(requestId);
            gptAnalyzeAnswer(requestId, results);

            // Processing
            exchange.sendResponseHeaders(102, -1);
            exchange.close();
        } else if (analyzeRequests.containsKey(requestId)) {
            CompletableFuture<Chat> results = analyzeRequests.remove(requestId);
            Chat response = results.join();

            // Open AI response
            Bitmap image;
            try {
                String msg = gptGetFinalResult(response);
                image = renderLatexToImage(msg, requestId);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
                return;
            }

            // Add width data
            ByteBuffer buff = ByteBuffer.allocate(image.pixels.length + 4);
            buff.putInt(image.width);
            buff.put(image.pixels, 0, image.pixels.length);
            byte[] outData = buff.array();

            exchange.sendResponseHeaders(200, outData.length);
            exchange.getResponseBody().write(outData);
            exchange.close();
        } else {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
        }
    }

    @Override
    public void handle(HttpExchange exchange) {
        System.out.println(exchange.getRequestURI() + " " + exchange.getRequestMethod());
        try {
            switch (exchange.getRequestMethod()) {
                case "POST" -> receiveImageFromClient(exchange);
                case "GET" -> getResult(exchange);
                default -> {
                    exchange.sendResponseHeaders(400, -1);
                    exchange.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
