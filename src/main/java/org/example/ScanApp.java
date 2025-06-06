package org.example;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class ScanApp {

    protected static final String OUTPUT_DIRECTORY_PATH = "C:\\temp\\scans";
    protected static final String NAPS2_CONSOLE_PATH = "\"C:\\Program Files\\NAPS2\\NAPS2.Console.exe\"";

    private static final Logger logger = LoggerFactory.getLogger(ScanApp.class);

    public static void main(String[] args) {
        Javalin app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors ->
                        cors.addRule(corsRule -> {
                                corsRule.anyHost();
                                corsRule.exposeHeader(Header.AUTHORIZATION);
        }))).start(8090);

        createOutputDirectory();

        app.post("/scan", ScanApp::scan);
    }

    private static void createOutputDirectory() {
        Path outputDirectoryPath = Paths.get(OUTPUT_DIRECTORY_PATH);

        if (!Files.exists(outputDirectoryPath)) {
            try {
                Files.createDirectories(outputDirectoryPath);
                logger.error("Output directory created: {}", OUTPUT_DIRECTORY_PATH);

            } catch (IOException e) {
                logger.error("Error trying to create output directory: {}", OUTPUT_DIRECTORY_PATH);
                System.exit(1);
            }
        }
    }

    protected static ProcessBuilder createProcessBuilder(String... command) {
        return new ProcessBuilder(command);
    }

    private static void scan(Context context) throws ScannerException {
        byte[] result = new byte[0];

        String fileName = UUID.randomUUID() + ".pdf";
        Path outputPath = Paths.get(OUTPUT_DIRECTORY_PATH, fileName);

        try {
            String[] command = {NAPS2_CONSOLE_PATH,
                    "-o", outputPath.toString(),
                    "--noprofile",
                    "--driver", "wia",
                    "--device", "EPSON",
                    "--source", "glass",
                    "--dpi", "100",
                    "--pagesize", "a4"
            };

            Process process = createProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            int returnCode = process.waitFor();

            if (returnCode != 0) {
                throw new ScannerException(ErrorMessage.NAPS_SCAN_ERROR);
            }

            if (!Files.exists(outputPath)) {
                throw new ScannerException(ErrorMessage.SCANNER_NOT_FOUND);
            }

            result = Files.readAllBytes(outputPath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

        } catch (Exception e) {
            throw new ScannerException(e.getMessage());

        } finally {
            try {
                if (Files.exists(outputPath)) {
                    Files.delete(outputPath);
                    logger.info("File deleted: {}", outputPath);
                }

            } catch (IOException e) {
                logger.error("Error trying to delete file: {}", outputPath);
                logger.error("Error message: {}", e.getMessage());
            }
        }

        context.result(result);
    }
}
