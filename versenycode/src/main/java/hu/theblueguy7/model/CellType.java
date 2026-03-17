package hu.theblueguy7.model;

public enum CellType {
    GROUND('.'), OBSTACLE('#'), BLUE('B'), YELLOW('Y'), GREEN('G'), START('S');

    private final char symbol;
    CellType(char symbol) { this.symbol = symbol; }
    public char getSymbol() { return symbol; }

    public boolean isMineral() {
        return this == BLUE || this == YELLOW || this == GREEN;
    }
    public boolean isPassable() {
        return this != OBSTACLE;
    }
}