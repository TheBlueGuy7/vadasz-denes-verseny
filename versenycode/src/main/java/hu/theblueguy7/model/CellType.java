package hu.theblueguy7.model;

public enum CellType {
    GROUND('.'),
    OBSTACLE('#'),
    BLUE('B'),
    YELLOW('Y'),
    GREEN('G'),
    START('S');

    private final char symbol;
    CellType(char symbol) { this.symbol = symbol; }
}
