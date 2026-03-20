package hu.theblueguy7.ui;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Launcher {

    public static void main(String[] args) throws Exception {
        if (System.getProperty("javafx.natives.ready") != null) {
            RoverUI.main(args);
            return;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");

        Path tempDir = extractNativeLibs(isWindows);
        if (tempDir == null) {
            System.err.println("Failed to extract native libraries, trying direct launch...");
            RoverUI.main(args);
            return;
        }

        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String jarPath = new File(
                Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        ).getAbsolutePath();

        String existingLibPath = System.getProperty("java.library.path", "");
        String sep = File.pathSeparator;
        String newLibPath = tempDir.toAbsolutePath() + (existingLibPath.isEmpty() ? "" : sep + existingLibPath);

        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.add("-Djava.library.path=" + newLibPath);
        cmd.add("-Djavafx.natives.ready=true");
        cmd.add("-Dprism.verbose=true");
        cmd.add("--enable-native-access=ALL-UNNAMED");
        cmd.add("-jar");
        cmd.add(jarPath);
        cmd.addAll(Arrays.asList(args));

        System.out.println("[Launcher] Extracting native libs to: " + tempDir.toAbsolutePath());
        System.out.println("[Launcher] Re-launching: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        Process proc = pb.start();
        int exitCode = proc.waitFor();

        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {}

        System.exit(exitCode);
    }

    private static Path extractNativeLibs(boolean isWindows) {
        try {
            Path tempDir = Files.createTempDirectory("javafx-natives");

            String suffix = isWindows ? ".dll" : ".so";
            boolean extracted = false;

            String jarPath = Launcher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            if (isWindows && jarPath.startsWith("/")) {
                jarPath = jarPath.substring(1);
            }

            try (var jarFile = new java.util.jar.JarFile(jarPath)) {
                var entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.contains("/") && name.endsWith(suffix)) {
                        try (InputStream in = jarFile.getInputStream(entry)) {
                            Path target = tempDir.resolve(name);
                            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                            extracted = true;
                        }
                    }
                }
            }

            if (extracted) {
                System.out.println("[Launcher] Extracted native libs: " +
                        Arrays.toString(tempDir.toFile().list()));
                return tempDir;
            } else {
                Files.delete(tempDir);
                return null;
            }
        } catch (Exception e) {
            System.err.println("[Launcher] Error extracting natives: " + e.getMessage());
            return null;
        }
    }
}
