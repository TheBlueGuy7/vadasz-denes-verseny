package hu.theblueguy7.model;

public enum CellType {
    GROUND('.'),
    OBSTACLE('#'),
    BLUE('B'),
    YELLOW('Y'),
    GREEN('G'),
    START('S');

    CellType(char symbol) {
    }
}
