import javafx.collections.ListChangeListener;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DownloaderAndZipper implements ListChangeListener<String> {

    // Utilizamos un Set para generar los números aleatorios y
    // aseguranos de que no se repitan
    private Set<String> generatedStrings = new HashSet<>();

    // Utilizamos una lista de futuros
    private List<Future<String>> futures = new ArrayList<>();

    // Override al método onChanged mediante el cual reaccionamos a los cambios en la lista
    // observable a la que haremos de listener
    @Override
    public void onChanged(Change<? extends String> change) {
        while (change.next()) {
            // SI se ha añadido un elemento a la lista observada ejecutamos las ordenes
            if (change.wasAdded()) {
                // Utilizamos la clave "PROCESS" para empezar a descargar y comprimir las páginas
                if (!change.getAddedSubList().get(0).equalsIgnoreCase("PROCESS")) {

                    // Sacamos el elemento que se ha añadido a la lista observada
                    String addedElement = change.getAddedSubList().get(0);
                    // Generamos un String aleatorio único para esta sesion
                    String randomString = generateRandomString(20);

                    // Cliente y petición HTTP para comunicarnos con la página que vamos a descargar
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(change.getAddedSubList().get(0)))
                            .GET()
                            .build();

                    // Guardamos el elemento modificado según el enunciado pese a que no lo vamos
                    // a utilizar en el programa, ya que seguimos un método más óptimo
                    // (Utilizamos los Strings ya recogidos anteriormente)
                    String elementoModificado = addedElement + randomString;

                    // Notificamos al usuario que hemos encolado una petición con un String único y aleatorio
                    System.out.println(addedElement + " encolado como " + randomString);

                    // Declaramos el future par "prometer" al programa que l petición HTTP devolverá un valor más tarde
                    Future<String> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            // Enviamos la petición
                            return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }).whenComplete((response, error) -> {
                        // Una vez se ha completado el Future guardamos el archivo con el String aleatorio
                        Path path = Paths.get("downloaded_files", randomString + ".html");
                        try {
                            Files.write(path, response.getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    // Añadimos a la lista de futuros para esperar a que toda la lista de futuros se haya
                    // completado en el main.
                    futures.add(future);
                }
            }
        }
    }

    // Método para esperar a que todos los Future se hayan completado
    public void waitForCompletion() {
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        // Esperamos a que todas las descargas se completen antes de comprimir
        allOf.join();

        // Comprimimos los archivos descargados en un archivo ZIP
        compressFiles();
    }

    // Método para comprimir los archivos una vez se reciba la orden de PROCESS por parte del usuario
    private void compressFiles() {
        try {
            // declaramos el nombre del archivo como compressed_files.zip
            Path zipFilePath = Paths.get("compressed_files.zip");

            // Guardamos todos los archivos de la carpeta "donwloaded_files" en el zip.
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
                Files.walk(Paths.get("downloaded_files"))
                        // Comprobamos que el archivo no es un directorio
                        .filter(path -> !Files.isDirectory(path))
                        // Añadimos cada archivo al zip
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(path.toString());
                            try {
                                zipOutputStream.putNextEntry(zipEntry);
                                Files.copy(path, zipOutputStream);
                                zipOutputStream.closeEntry();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Método que devuelve un String aleatorio único para esta sesión
    private String generateRandomString(int length) {
        // String con un diccionario simple para llamar sus posiciones de forma aleatorioa.
        String caracteres = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        // Utilizando StringBuilder añadimos los carácteres que elegimos aleatoriamente
        // del anterior diccionario. SI ya existe en el set, se vuelve a generar otro String
        // hasta que sea único.
        while (true) {
            for (int i = 0; i < length; i++) {
                int index = random.nextInt(caracteres.length());
                // añadimos al final del string
                sb.append(caracteres.charAt(index));
            }

            // Convertimos el StringBuilder a un String
            String generatedString = sb.toString();

            if (generatedStrings.add(generatedString)) {
                return generatedString;
            }

            sb.setLength(0);
        }
    }
}

