package hu.theblueguy7;

import hu.theblueguy7.model.CellType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MapParser {
    public static CellType[][] loadMap(String fileName, int sizeX, int sizeY) throws IOException {
        CellType[][] grid = new CellType[sizeX][sizeY];
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        int row = 0;

        while ((line = br.readLine()) != null && row < sizeY) {
            String[] tokens = line.split(",");
            for (int col = 0; col < Math.min(tokens.length, sizeX); col++) {
                char symbol = tokens[col].trim().charAt(0);
                grid[row][col] = parseCell(symbol);

                if (symbol == 'S') {
                    System.out.println("Start pozíció: " + row + "," + col);
                }
            }
            row++;
        }
        br.close();
        return grid;
    }

    private static CellType parseCell(char symbol) {
        return switch (symbol) {
            case '#' -> CellType.OBSTACLE;
            case 'B' -> CellType.BLUE;
            case 'Y' -> CellType.YELLOW;
            case 'G' -> CellType.GREEN;
            case 'S' -> CellType.START;
            default -> CellType.GROUND;
        };
    }
    }
}
