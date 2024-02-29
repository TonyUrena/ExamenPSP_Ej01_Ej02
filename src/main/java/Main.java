import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String url;

        // Verificamos y creamos el directorio "downloaded_files" si no existe
        Path downloadedFilesDirectory = Paths.get("downloaded_files");
        if (!Files.exists(downloadedFilesDirectory)) {
            try {
                Files.createDirectories(downloadedFilesDirectory);
            } catch (IOException e) {
                throw new RuntimeException("Error al crear el directorio 'downloaded_files'", e);
            }
        }

        // Lista a la que observaremos en busca de cambios.
        ObservableList<String> urlList = FXCollections.observableArrayList();

        DownloaderAndZipper downloaderAndZipper = new DownloaderAndZipper();
        urlList.addListener(downloaderAndZipper);

        // Pedimos las URL al usuario hasta que escriba la palabra PROCESS.
        // He elegido este método porque al esperar un String vacío daba problemas.
        do {
            System.out.println("Introuduce una URL (escribe PROCESS para descargar y comprimir las URL)");
            url = sc.nextLine();

            urlList.add(url);
        } while (!url.equalsIgnoreCase("PROCESS"));

        // EJecutamos el método de DownloaderAndZipper que contiene la espera para que todos los FUture de la lista
        // se hayan terminado de ejecutar.
        downloaderAndZipper.waitForCompletion();
    }
}
