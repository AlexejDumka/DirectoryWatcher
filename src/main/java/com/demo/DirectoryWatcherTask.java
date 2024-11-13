package com.demo;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class DirectoryWatcherTask implements Runnable {
    private final Path path;

    public DirectoryWatcherTask(Path path) {
        this.path = path;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

            System.out.println("Наблюдение за директорией " + path + " началось...");

            while (true) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Наблюдатель был прерван.");
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    handleEvent(event);
                }
                key.reset();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод обработки событий файловой системы
    private void handleEvent(WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();
        Path filePath = (Path) event.context();

        // Логика обработки событий
        System.out.println("Событие: " + kind + " для файла: " + filePath);
    }

    public static void main(String[] args) {
        // Укажите путь к директории, которую хотите отслеживать
        Path directoryPath = Paths.get("C:/example/directory");

        // Создаем виртуальный поток или обычный поток, если поддержка виртуальных недоступна
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        DirectoryWatcherTask watcherTask = new DirectoryWatcherTask(directoryPath);

        // Запуск задачи наблюдателя в отдельном потоке
        executor.submit(watcherTask);

        // Поддержание выполнения программы
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Программа завершает работу...");
            executor.shutdownNow();
        }));

        try {
            while (!executor.isShutdown()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
