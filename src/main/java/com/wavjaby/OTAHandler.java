package com.wavjaby;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class OTAHandler implements HttpHandler {
    final File file;
    long fileLastModified;

    public OTAHandler(String path) {
        file = new File(path);
        fileLastModified = file.lastModified();
        if(file.exists())
            System.out.println("OTA file ready");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (fileLastModified == file.lastModified()) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        fileLastModified = file.lastModified();
        System.out.println(exchange.getRequestURI());

        byte[] fileData = Files.readAllBytes(file.toPath());
        exchange.sendResponseHeaders(200, fileData.length);
        exchange.getResponseBody().write(fileData);
        exchange.close();
    }
}
