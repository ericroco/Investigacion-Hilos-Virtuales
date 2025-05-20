package com.example;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Path filePath = Paths.get("urls_parcial1-1.txt");
        List<String> urls = Files.readAllLines(filePath);

        List<Result> results = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        
        // Crear una lista para almacenar las tareas futuras
        List<Future<?>> futures = new ArrayList<>();

        urls.forEach(url -> {
            // Almacenar cada Future devuelto por submit()
            Future<?> future = executor.submit(() -> {
                try {
                    int count = UrlProcessor.process(url);
                    results.add(new Result(url, count));
                } catch (Exception e) {
                    System.err.println("Error procesando " + url + ": " + e.getMessage());
                    results.add(new Result(url, -1)); // Error
                }
            });
            futures.add(future);
        });

        executor.shutdown();
        
        // Esperar a que todas las tareas terminen
        for (Future<?> future : futures) {
            try {
                future.get(); // Espera a que cada tarea termine
            } catch (Exception e) {
                System.err.println("Error esperando tarea: " + e.getMessage());
            }
        }
        
        // Asegurarse de que el executor termine
        if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
            System.err.println("El executor no terminó en el tiempo esperado");
        }

        saveResultsToCsv(results, "resultados.csv");
        System.out.println("Proceso terminado. Resultados guardados en 'resultados.csv'");
    }

    private static void saveResultsToCsv(List<Result> results, String filename) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            writer.write("url,cantidad\n");
            for (Result r : results) {
                writer.write(r.url + "," + r.count + "\n");
            }
        }
    }

    private static class Result {
        String url;
        int count;

        Result(String url, int count) {
            this.url = url;
            this.count = count;
        }
    }
}