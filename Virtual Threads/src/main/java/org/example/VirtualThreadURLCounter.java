package org.example;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SubmissionPublisher;

public class VirtualThreadURLCounter {

    private static final String FILE_PATH = "urls.txt";
    private static final String CSV_OUTPUT = "url_counts.csv";

    public static void main(String[] args) {
        // Crear un publicador para emitir resultados
        SubmissionPublisher<Result> publisher = new SubmissionPublisher<>();

        // Suscribir un suscriptor para procesar los resultados
        publisher.subscribe(new Flow.Subscriber<Result>() {
            private Flow.Subscription subscription;
            private BufferedWriter writer;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                try {
                    this.writer = new BufferedWriter(new FileWriter(CSV_OUTPUT));
                    this.writer.write("URL,Count,Error\n"); // Cabecera del CSV
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                subscription.request(Integer.MAX_VALUE); // Solicitar todos los elementos
            }

            @Override
            public void onNext(Result result) {
                try {
                    if (result.error == null) {
                        writer.write(String.format("%s,%d,%s\n", result.url, result.count, ""));
                    } else {
                        writer.write(String.format("%s,%d,%s\n", result.url, result.count, result.error));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onComplete() {
                try {
                    writer.close();
                    System.out.println("Resultados guardados en " + CSV_OUTPUT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Crear un executor de virtual threads
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Leer todas las URLs del archivo
            List<String> urls = readUrlsFromFile(FILE_PATH);

            // Procesar cada URL en un virtual thread
            for (String url : urls) {
                executor.submit(() -> {
                    try {
                        int count = countUrlsInPage(url);
                        publisher.submit(new Result(url, count));
                    } catch (Exception e) {
                        publisher.submit(new Result(url, -1, e.getMessage()));
                    }
                });
            }
        }

        // Cerrar el publicador
        publisher.close();
    }

    private static List<String> readUrlsFromFile(String filePath) {
        List<String> urls = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    urls.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo el archivo: " + e.getMessage());
            System.exit(1);
        }
        return urls;
    }

    private static int countUrlsInPage(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        // Contar las URLs en el cuerpo de la página (simplificado)
        int count = 0;
        int index = 0;
        while ((index = body.indexOf("href=\"", index)) != -1) {
            count++;
            index += "href=\"".length();
        }

        return count;
    }

    private static class Result {
        public final String url;
        public final int count;
        public final String error;

        public Result(String url, int count) {
            this.url = url;
            this.count = count;
            this.error = null;
        }

        public Result(String url, int count, String error) {
            this.url = url;
            this.count = count;
            this.error = error;
        }
    }
}