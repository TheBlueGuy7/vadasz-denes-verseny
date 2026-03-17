package hu.theblueguy7;

import hu.theblueguy7.model.CellType;

import java.io.*;
import java.nio.file.*;

public class FileManager {
    private static final String LOG_FILE = "simulation.log";

    // terkep betoltese (atalakitva)
    public static CellType[][] loadMap(String path) throws IOException {
        CellType[][] map = new CellType[50][50];
        BufferedReader br = new BufferedReader(new FileReader(path));
        for (int i = 0; i < 50; i++) {
            String line = br.readLine();
            if (line == null) break;
            String[] tokens = line.split(",");
            for (int j = 0; j < 50; j++) {
                map[i][j] = parse(tokens[j].trim().charAt(0));
            }
        }
        br.close();
        return map;
    }

    private static CellType parse(char c) {
        for (CellType t : CellType.values()) if (t.getSymbol() == c) return t;
        return CellType.GROUND;
    }

    public static void log(String message) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)))) {
            out.println(message);
        } catch (IOException e) {
            System.err.println("Hiba a log fájl írásakor: " + e.getMessage());
        }
    }

    public static void resetLog() {
        try {
            Files.deleteIfExists(Paths.get(LOG_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}