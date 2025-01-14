package com.demo;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryWatcherTask implements Runnable {
    private static final Logger logger = Logger.getLogger(DirectoryWatcherTask.class.getName());
    private final Path pathIN;
    private final Path pathOUT;

    public DirectoryWatcherTask(Path pathIN, Path pathOUT) {
        this.pathIN = pathIN;
        this.pathOUT = pathOUT;
    }

    public static void handleError(String message, Exception e) {
        logger.log(Level.SEVERE, message, e);
    }

    public static void convertFile(Path sourcePath, Path targetPath) {
        String extension = getExtension(sourcePath.toString());
        if (extension.equalsIgnoreCase(".pdf")) {
            convertPDFToText(sourcePath.toString(), targetPath.toString());
        } else {
            logger.warning("Unsupported file type: " + extension);
        }
    }

    private static void convertPDFToText(String filePath, String targetPath) {
        String uuid = UUID.randomUUID().toString();
        File pdfFile = new File(filePath);
        File txtFile = new File(targetPath.replace(".txt", "_" + uuid + ".txt"));

        try {
            PDDocument document = PDDocument.load(pdfFile, MemoryUsageSetting.setupTempFileOnly());
            PDFTextStripper pdfStripper = new PDFTextStripper();

            pdfStripper.setLineSeparator("\n");
            pdfStripper.setParagraphStart("\n");
            pdfStripper.setSortByPosition(true);
            String content = pdfStripper.getText(document);
            document.close();

            saveTextToFile(content, txtFile);
            if (pdfFile.delete()) {
                logger.info("File is successfully deleted: " + pdfFile.getName());
            } else {
                logger.warning("An error occurred while deleting the PDF file: " + pdfFile.getName());
            }

        } catch (IOException e) {
            handleError("An error occurred when converting file: " + filePath, e);
        }
    }

    private static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }

    private static void saveTextToFile(String textContent, File file) {
        logger.info("Saving extracted text to file: " + file.getName());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(textContent);
        } catch (IOException e) {
            handleError("Error saving file: " + file.getName(), e);
        }
    }

    private static String getBaseFileName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    public static void main(String[] args) {
        Path pdfDirectoryPath = Paths.get(System.getProperty("user.dir"), "IN");
        Path txtDirectoryPath = Paths.get(System.getProperty("user.dir"), "OUT");

        try {
            if (!Files.exists(pdfDirectoryPath)) {
                Files.createDirectory(pdfDirectoryPath);
            }
            if (!Files.exists(txtDirectoryPath)) {
                Files.createDirectory(txtDirectoryPath);
            }
        } catch (IOException e) {
            handleError("Error creating directories :", e);
            return;
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        DirectoryWatcherTask watcherTask = new DirectoryWatcherTask(pdfDirectoryPath, txtDirectoryPath);

        executor.submit(watcherTask);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Program is shutdown...");
            executor.shutdownNow();
        }));

        try {
            while (!executor.isShutdown()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleError("Main thread was interrupted.", e);
        }
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            pathIN.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

            logger.info("Watching for folder " + pathIN + " is started");

            while (true) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warning("Watcher was interrupted.");
                    return;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    handleEvent(event);
                }
                key.reset();
            }
        } catch (IOException e) {
            handleError("Error when setting up WatchService.", e);
        }
    }

    private void handleEvent(WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();

        if (kind == StandardWatchEventKinds.OVERFLOW) {
            return;
        }

        Path filePath = (Path) event.context();
        Path pdfFilePath = pathIN.resolve(filePath);
        Path txtFilePath = pathOUT.resolve(getBaseFileName(filePath.toString()) + ".txt");

        logger.info("Event: " + kind + " for file: " + pdfFilePath);

        try {
            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                convertFile(pdfFilePath, txtFilePath);
            }
        } catch (Exception e) {
            handleError("Error when handling event: " + kind, e);
        }
    }
}
