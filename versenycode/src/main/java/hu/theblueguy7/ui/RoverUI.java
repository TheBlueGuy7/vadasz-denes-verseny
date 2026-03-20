package hu.theblueguy7.ui;

import hu.theblueguy7.FileManager;
import hu.theblueguy7.SimulationEngine;
import hu.theblueguy7.model.CellType;
import hu.theblueguy7.model.SimulationFrame;
import hu.theblueguy7.model.Speed;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.geometry.Point2D;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Line;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class RoverUI extends Application {

    // === COLOR THEME ===
    private static final String BG_DARK      = "#0f0f1a";
    private static final String BG_SURFACE   = "#1a1b2e";
    private static final String BG_CARD      = "#232438";
    private static final String BG_ELEVATED  = "#2c2d44";
    private static final String ACCENT       = "#7c5cfc";
    private static final String ACCENT_TEAL  = "#00d4aa";
    private static final String ACCENT_AMBER = "#ffb347";
    private static final String ACCENT_RED   = "#ff6b6b";
    private static final String TEXT_PRIMARY  = "#e8e8f0";
    private static final String TEXT_SECONDARY= "#8888a0";
    private static final String BORDER_COLOR = "#2e2f48";

    private static final Color CLR_BG_DARK    = Color.web(BG_DARK);
    private static final Color CLR_SURFACE    = Color.web(BG_SURFACE);
    private static final Color CLR_CARD       = Color.web(BG_CARD);
    private static final Color CLR_ACCENT     = Color.web(ACCENT);
    private static final Color CLR_TEAL       = Color.web(ACCENT_TEAL);
    private static final Color CLR_AMBER      = Color.web(ACCENT_AMBER);
    private static final Color CLR_RED        = Color.web(ACCENT_RED);
    private static final Color CLR_TEXT       = Color.web(TEXT_PRIMARY);
    private static final Color CLR_TEXT_SEC   = Color.web(TEXT_SECONDARY);

    // map colors
    private static final Color MAP_GROUND    = Color.web("#1e1f30");
    private static final Color MAP_OBSTACLE  = Color.web("#12121e");
    private static final Color MAP_BLUE      = Color.web("#4a7cff");
    private static final Color MAP_YELLOW    = Color.web("#ffc845");
    private static final Color MAP_GREEN     = Color.web("#3de08a");
    private static final Color MAP_START     = Color.web(ACCENT);
    private static final Color MAP_GRID      = Color.web("#2a2b40", 0.4);
    private static final Color PATH_HISTORY  = Color.web(ACCENT, 0.5);
    private static final Color PATH_FUTURE   = Color.web(ACCENT_AMBER, 0.6);
    private static final Color ROVER_COLOR   = Color.web(ACCENT_TEAL);
    private static final Color ROVER_GLOW    = Color.web(ACCENT_TEAL, 0.25);

    private List<SimulationFrame> frames;
    private int currentFrameIndex = 0;
    private Timeline playbackTimeline;
    private boolean isPlaying = false;

    // UI components
    private Label speedLabel, speedDetailLabel, gemsLabel, batteryLabel, positionLabel, timeLabel, dayNightLabel, distanceLabel;
    private ProgressBar batteryBar;
    private TextArea fileLogArea, consoleLogArea;
    private LineChart<Number, Number> chart;
    private XYChart.Series<Number, Number> batterySeries, speedSeries;
    private Line chartIndicator;
    private Canvas mapCanvas;
    private Slider timeSlider;
    private Button playPauseBtn;
    private ComboBox<String> speedSelector;
    private SubScene modelScene;
    private Pane chartPane;

    @Override
    public void start(Stage primaryStage) {
        Dialog<ButtonType> setupDialog = new Dialog<>();
        setupDialog.setTitle("Mars Rover Simulation");
        setupDialog.setHeaderText("Configure Simulation");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(24));
        grid.setStyle("-fx-background-color: " + BG_SURFACE + ";");

        TextField mapField = new TextField("./map.csv");
        mapField.setStyle(fieldStyle());
        Button browseBtn = new Button("Browse");
        browseBtn.setStyle(btnStyle());
        TextField hoursField = new TextField("24");
        hoursField.setStyle(fieldStyle());

        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setInitialDirectory(new File("."));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showOpenDialog(setupDialog.getOwner());
            if (f != null) mapField.setText(f.getPath());
        });

        Label mapLbl = new Label("Map file");
        mapLbl.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px;");
        Label hrsLbl = new Label("Duration (hours)");
        hrsLbl.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px;");

        grid.add(mapLbl, 0, 0);
        grid.add(mapField, 1, 0);
        grid.add(browseBtn, 2, 0);
        grid.add(hrsLbl, 0, 1);
        grid.add(hoursField, 1, 1);

        setupDialog.getDialogPane().setContent(grid);
        setupDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // style the entire dialog via stylesheet
        DialogPane dp = setupDialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());

        setupDialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String mapPath = mapField.getText();
                    int hours = Integer.parseInt(hoursField.getText());
                    CellType[][] map = FileManager.loadMap(mapPath);
                    int[] startPos = findStart(map);
                    SimulationEngine engine = new SimulationEngine(map, startPos[0], startPos[1]);
                    frames = engine.preCompute(hours);
                    buildUI(primaryStage);
                    showFrame(0);
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
                }
            }
        });
    }

    private int[] findStart(CellType[][] map) {
        for (int i = 0; i < map.length; i++)
            for (int j = 0; j < map[i].length; j++)
                if (map[i][j] == CellType.START) return new int[]{i, j};
        return new int[]{0, 0};
    }

    private void buildUI(Stage stage) {
        // ========== LEFT SIDE ==========

        // --- 3D Model ---
        Group modelGroup = new Group();
        try {
            InputStream objStream = getClass().getResourceAsStream("/models/marsrover.obj");
            if (objStream == null) objStream = new FileInputStream("src/main/resources/models/marsrover.obj");
            MeshView rover3D = ObjModelLoader.load(objStream);
            rover3D.setScaleX(0.6);
            rover3D.setScaleY(-0.6); // flip upside down
            rover3D.setScaleZ(0.6);
            modelGroup.getChildren().add(rover3D);

            RotateTransition rotate = new RotateTransition(Duration.seconds(20), rover3D);
            rotate.setAxis(Rotate.Y_AXIS);
            rotate.setFromAngle(0);
            rotate.setToAngle(360);
            rotate.setCycleCount(Animation.INDEFINITE);
            rotate.setInterpolator(Interpolator.LINEAR);
            rotate.play();
        } catch (Exception e) {
            System.err.println("Could not load 3D model: " + e.getMessage());
        }

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-200);
        camera.setTranslateY(40); // move camera down to shift model up
        camera.setFieldOfView(35);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);

        modelScene = new SubScene(modelGroup, 280, 240, true, SceneAntialiasing.BALANCED);
        modelScene.setFill(CLR_BG_DARK);
        modelScene.setCamera(camera);

        StackPane modelContainer = new StackPane(modelScene);
        modelContainer.setStyle(cardStyle());
        modelContainer.setMinSize(280, 240);
        modelContainer.setMaxSize(300, 260);

        // --- Stats ---
        speedLabel = styledValueLabel(ACCENT);
        speedDetailLabel = new Label("E = 2.0");
        speedDetailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_SECONDARY + ";");
        speedDetailLabel.setAlignment(Pos.CENTER);
        speedDetailLabel.setMinWidth(70);

        gemsLabel = styledValueLabel(ACCENT_TEAL);
        batteryLabel = styledValueLabel(TEXT_PRIMARY);
        distanceLabel = styledValueLabel(TEXT_PRIMARY);

        batteryBar = new ProgressBar(1.0);
        batteryBar.setMaxWidth(Double.MAX_VALUE);
        batteryBar.setPrefHeight(12);
        batteryBar.getStyleClass().add("battery-bar");

        dayNightLabel = styledValueLabel(ACCENT_AMBER);

        VBox batteryContent = new VBox(6, batteryBar, batteryLabel);
        batteryContent.setAlignment(Pos.CENTER);
        batteryContent.setFillWidth(true);

        VBox speedContent = new VBox(2, speedLabel, speedDetailLabel);
        speedContent.setAlignment(Pos.CENTER);
        VBox speedBox = statCard("SPEED", speedContent);
        VBox gemsBox = statCard("MINERALS", gemsLabel);
        VBox batteryBox = statCard("BATTERY", batteryContent);
        VBox dayBox = statCard("CYCLE", dayNightLabel);
        VBox distBox = statCard("DISTANCE", distanceLabel);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(10);
        statsGrid.setVgap(10);
        for (int c = 0; c < 3; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 3);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            statsGrid.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < 2; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(50);
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            statsGrid.getRowConstraints().add(rc);
        }
        speedBox.setMaxWidth(Double.MAX_VALUE);
        speedBox.setMaxHeight(Double.MAX_VALUE);
        gemsBox.setMaxWidth(Double.MAX_VALUE);
        gemsBox.setMaxHeight(Double.MAX_VALUE);
        distBox.setMaxWidth(Double.MAX_VALUE);
        distBox.setMaxHeight(Double.MAX_VALUE);
        batteryBox.setMaxWidth(Double.MAX_VALUE);
        batteryBox.setMaxHeight(Double.MAX_VALUE);
        batteryBox.setFillWidth(true);
        dayBox.setMaxWidth(Double.MAX_VALUE);
        dayBox.setMaxHeight(Double.MAX_VALUE);

        statsGrid.add(speedBox, 0, 0);
        statsGrid.add(gemsBox, 1, 0);
        statsGrid.add(distBox, 2, 0);
        statsGrid.add(dayBox, 0, 1);
        statsGrid.add(batteryBox, 1, 1, 2, 1);

        VBox statsPanel = new VBox(statsGrid);
        VBox.setVgrow(statsGrid, Priority.ALWAYS);

        HBox topRow = new HBox(12, modelContainer, statsPanel);
        topRow.setPadding(new Insets(12));
        topRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(statsPanel, Priority.ALWAYS);

        // --- Logs ---
        fileLogArea = createLogArea();
        fileLogArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_PRIMARY + ";");

        consoleLogArea = createLogArea();
        consoleLogArea.setWrapText(true);
        consoleLogArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 10px; -fx-text-fill: " + ACCENT_AMBER + ";");

        VBox fileLogBox = logPanel("FILE LOG", fileLogArea);
        VBox consoleLogBox = logPanel("CONSOLE", consoleLogArea);

        HBox logsRow = new HBox(10, fileLogBox, consoleLogBox);
        logsRow.setPadding(new Insets(0, 12, 0, 12));
        HBox.setHgrow(fileLogBox, Priority.ALWAYS);
        HBox.setHgrow(consoleLogBox, Priority.SOMETIMES);
        fileLogBox.setMaxWidth(Double.MAX_VALUE);
        consoleLogBox.setMaxWidth(Double.MAX_VALUE);
        fileLogBox.prefWidthProperty().bind(logsRow.widthProperty().multiply(0.70));
        consoleLogBox.prefWidthProperty().bind(logsRow.widthProperty().multiply(0.28));

        // --- Chart ---
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time (h)");
        xAxis.setTickLabelFill(CLR_TEXT_SEC);
        xAxis.setStyle("-fx-tick-label-fill: " + TEXT_SECONDARY + ";");

        NumberAxis yAxis = new NumberAxis(0, 110, 10);
        yAxis.setLabel("Value");
        yAxis.setTickLabelFill(CLR_TEXT_SEC);

        chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setPrefHeight(250);
        chart.setMinHeight(220);

        batterySeries = new XYChart.Series<>();
        batterySeries.setName("Battery %");
        speedSeries = new XYChart.Series<>();
        speedSeries.setName("Speed (x33)");
        chart.getData().addAll(batterySeries, speedSeries);

        chartIndicator = new Line();
        chartIndicator.setStroke(CLR_ACCENT);
        chartIndicator.setStrokeWidth(2);
        chartIndicator.setOpacity(0.8);
        chartIndicator.setMouseTransparent(true);

        chartPane = new Pane(chart, chartIndicator);
        chartPane.setPrefHeight(250);
        chartPane.setMinHeight(220);
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.prefWidthProperty().bind(chartPane.widthProperty());
        chart.prefHeightProperty().bind(chartPane.heightProperty());

        VBox chartContainer = new VBox(chartPane);
        chartContainer.setPadding(new Insets(0, 12, 12, 12));
        VBox.setVgrow(chartPane, Priority.ALWAYS);

        VBox leftPanel = new VBox(8, topRow, logsRow, chartContainer);
        leftPanel.setStyle("-fx-background-color: " + BG_DARK + ";");
        VBox.setVgrow(logsRow, Priority.ALWAYS);
        VBox.setVgrow(chartContainer, Priority.SOMETIMES);

        // ========== RIGHT SIDE (MAP) ==========
        mapCanvas = new Canvas(600, 600);

        positionLabel = new Label("Pos: (0, 0)");
        positionLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY +
                "; -fx-background-color: " + BG_CARD + "cc; -fx-padding: 6 10; -fx-background-radius: 6;");

        StackPane mapStack = new StackPane(mapCanvas, positionLabel);
        StackPane.setAlignment(positionLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(positionLabel, new Insets(8));

        mapStack.widthProperty().addListener((obs, o, n) -> {
            double size = Math.min(n.doubleValue(), mapStack.getHeight());
            mapCanvas.setWidth(size);
            if (frames != null && currentFrameIndex < frames.size()) drawMap(frames.get(currentFrameIndex));
        });
        mapStack.heightProperty().addListener((obs, o, n) -> {
            double size = Math.min(mapStack.getWidth(), n.doubleValue());
            mapCanvas.setHeight(size);
            if (frames != null && currentFrameIndex < frames.size()) drawMap(frames.get(currentFrameIndex));
        });

        VBox mapPanel = new VBox(mapStack);
        mapPanel.setPadding(new Insets(12));
        mapPanel.setStyle("-fx-background-color: " + BG_SURFACE + ";");
        VBox.setVgrow(mapStack, Priority.ALWAYS);

        // ========== TIMELINE CONTROLS ==========
        playPauseBtn = controlBtn("Play");
        playPauseBtn.setPrefWidth(90);
        playPauseBtn.setOnAction(e -> togglePlayback());

        Button stepBackBtn = controlBtn("<<");
        stepBackBtn.setOnAction(e -> { if (currentFrameIndex > 0) showFrame(currentFrameIndex - 1); });

        Button stepFwdBtn = controlBtn(">>");
        stepFwdBtn.setOnAction(e -> { if (currentFrameIndex < frames.size() - 1) showFrame(currentFrameIndex + 1); });

        Button resetBtn = controlBtn("Reset");
        resetBtn.setOnAction(e -> { stopPlayback(); consoleLogArea.clear(); showFrame(0); });

        timeSlider = new Slider(0, frames.size() - 1, 0);
        timeSlider.setBlockIncrement(1);
        timeSlider.setMajorTickUnit(frames.size() / 10.0);
        timeSlider.setShowTickLabels(false);
        timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (timeSlider.isValueChanging()) showFrame(newVal.intValue());
        });

        timeLabel = new Label("0.0 h");
        timeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-min-width: 70; -fx-font-family: 'monospace';");

        speedSelector = new ComboBox<>();
        speedSelector.getItems().addAll("0.5x", "1x", "2x", "5x", "10x", "50x");
        speedSelector.setValue("1x");
        speedSelector.setOnAction(e -> updatePlaybackSpeed());

        Label speedLbl = new Label("Speed:");
        speedLbl.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px;");

        HBox controls = new HBox(10, resetBtn, stepBackBtn, playPauseBtn, stepFwdBtn,
                timeSlider, timeLabel, speedLbl, speedSelector);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10, 16, 10, 16));
        controls.setStyle("-fx-background-color: " + BG_SURFACE + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1 0 0 0;");
        HBox.setHgrow(timeSlider, Priority.ALWAYS);

        // ========== MAIN LAYOUT ==========
        HBox mainContent = new HBox(0, leftPanel, mapPanel);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        HBox.setHgrow(mapPanel, Priority.ALWAYS);
        leftPanel.setMinWidth(0);
        mapPanel.setMinWidth(0);
        leftPanel.prefWidthProperty().bind(mainContent.widthProperty().multiply(0.5));
        mapPanel.prefWidthProperty().bind(mainContent.widthProperty().multiply(0.5));

        VBox root = new VBox(0, mainContent, controls);
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        Scene scene = new Scene(root, 1500, 900);
        scene.setFill(CLR_BG_DARK);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());

        stage.setTitle("Mars Rover Simulation");
        stage.setScene(scene);
        stage.show();

        populateChartData();
    }

    // ========== STYLING HELPERS ==========

    private String cardStyle() {
        return "-fx-background-color: " + BG_CARD + "; -fx-background-radius: 10; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 10;";
    }

    private String fieldStyle() {
        return "-fx-background-color: " + BG_ELEVATED + "; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-prompt-text-fill: " + TEXT_SECONDARY + "; -fx-background-radius: 6;";
    }

    private String btnStyle() {
        return "-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;";
    }

    private Label styledValueLabel(String color) {
        Label l = new Label("--");
        l.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        l.setMinWidth(70);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private VBox statCard(String title, Node content) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        VBox box = new VBox(6, t, content);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(14));
        box.setStyle(cardStyle());
        box.setMinHeight(100);
        box.setPrefHeight(110);
        return box;
    }

    private TextArea createLogArea() {
        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_PRIMARY + ";");
        return ta;
    }

    private VBox logPanel(String title, TextArea area) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        VBox box = new VBox(6, t, area);
        VBox.setVgrow(area, Priority.ALWAYS);
        return box;
    }

    private Button controlBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + BG_ELEVATED + "; -fx-text-fill: " + TEXT_PRIMARY +
                "; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + BG_ELEVATED + "; -fx-text-fill: " + TEXT_PRIMARY +
                "; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;"));
        return b;
    }

    // ========== DATA ==========

    private void populateChartData() {
        batterySeries.getData().clear();
        speedSeries.getData().clear();
        for (int i = 0; i < frames.size(); i++) {
            SimulationFrame f = frames.get(i);
            batterySeries.getData().add(new XYChart.Data<>(f.timeHours, f.battery));
            speedSeries.getData().add(new XYChart.Data<>(f.timeHours, f.speed.velocity * 33.0));
        }
    }

    private void updateChartIndicator() {
        if (frames == null || frames.isEmpty()) return;
        SimulationFrame frame = frames.get(currentFrameIndex);

        javafx.application.Platform.runLater(() -> {
            NumberAxis xAxis = (NumberAxis) chart.getXAxis();
            Node plotArea = chart.lookup(".chart-plot-background");
            if (plotArea == null) return;

            double xInAxis = xAxis.getDisplayPosition(frame.timeHours);
            Point2D axisInScene = xAxis.localToScene(xInAxis, 0);
            Point2D axisInPane = chartPane.sceneToLocal(axisInScene);

            Bounds plotBounds = plotArea.localToScene(plotArea.getBoundsInLocal());
            Bounds plotInPane = chartPane.sceneToLocal(plotBounds);

            chartIndicator.setStartX(axisInPane.getX());
            chartIndicator.setEndX(axisInPane.getX());
            chartIndicator.setStartY(plotInPane.getMinY());
            chartIndicator.setEndY(plotInPane.getMaxY());
        });
    }

    // ========== FRAME DISPLAY ==========

    private void showFrame(int index) {
        if (index < 0 || index >= frames.size()) return;
        currentFrameIndex = index;
        SimulationFrame frame = frames.get(index);

        // stats
        speedLabel.setText(frame.speed.toString());
        String speedColor = switch (frame.speed) {
            case SLOW -> ACCENT_TEAL;
            case NORMAL -> ACCENT_AMBER;
            case FAST -> ACCENT_RED;
        };
        speedLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + speedColor + ";");
        double energyCost = 2.0 * Math.pow(frame.speed.velocity, 2);
        speedDetailLabel.setText("E = " + String.format("%.0f", energyCost) + " /step");

        gemsLabel.setText(String.valueOf(frame.mineralsCollected));
        distanceLabel.setText(String.valueOf(frame.totalDistance));

        batteryBar.setProgress(frame.battery / 100.0);
        batteryLabel.setText(String.format("%.1f%%", frame.battery));
        String batColor = frame.battery > 60 ? ACCENT_TEAL : frame.battery > 25 ? ACCENT_AMBER : ACCENT_RED;
        batteryBar.lookup(".bar").setStyle("-fx-background-color: " + batColor + ";");
        batteryLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + batColor + ";");

        dayNightLabel.setText(frame.isDay ? "DAY" : "NIGHT");
        dayNightLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " +
                (frame.isDay ? ACCENT_AMBER : ACCENT) + ";");

        positionLabel.setText(String.format("Pos: (%d, %d)", frame.roverX, frame.roverY));
        timeLabel.setText(String.format("%.1f h", frame.timeHours));

        StringBuilder fileLog = new StringBuilder();
        for (int i = 0; i <= index; i++) {
            fileLog.append(frames.get(i).fileLogLine).append("\n");
        }
        fileLogArea.setText(fileLog.toString());
        fileLogArea.positionCaret(fileLogArea.getLength());

        // console log
        if (frame.consoleLogLine != null) {
            consoleLogArea.appendText("[" + String.format("%.1f h", frame.timeHours) + "] " + frame.consoleLogLine + "\n");
            consoleLogArea.positionCaret(consoleLogArea.getLength());
        }

        // slider
        if (!timeSlider.isValueChanging()) timeSlider.setValue(index);

        // chart indicator
        updateChartIndicator();

        // map
        drawMap(frame);
    }

    // ========== MAP DRAWING ==========

    private void drawMap(SimulationFrame frame) {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        double w = mapCanvas.getWidth();
        double h = mapCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        double cellW = w / 50.0;
        double cellH = h / 50.0;

        gc.setFill(CLR_BG_DARK);
        gc.fillRect(0, 0, w, h);

        // cells
        CellType[][] map = frame.mapSnapshot;
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                Color color = switch (map[i][j]) {
                    case GROUND -> MAP_GROUND;
                    case OBSTACLE -> MAP_OBSTACLE;
                    case BLUE -> MAP_BLUE;
                    case YELLOW -> MAP_YELLOW;
                    case GREEN -> MAP_GREEN;
                    case START -> MAP_START;
                };
                gc.setFill(color);
                gc.fillRect(j * cellW, i * cellH, cellW - 0.5, cellH - 0.5);
            }
        }

        gc.setStroke(PATH_HISTORY);
        gc.setLineWidth(2.5);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        if (currentFrameIndex > 0) {
            SimulationFrame prev = frames.get(0);
            for (int i = 1; i <= currentFrameIndex; i++) {
                SimulationFrame cur = frames.get(i);
                if (cur.roverX != prev.roverX || cur.roverY != prev.roverY) {
                    double x1 = prev.roverY * cellW + cellW / 2;
                    double y1 = prev.roverX * cellH + cellH / 2;
                    double x2 = cur.roverY * cellW + cellW / 2;
                    double y2 = cur.roverX * cellH + cellH / 2;
                    gc.strokeLine(x1, y1, x2, y2);
                }
                prev = cur;
            }
        }

        if (frame.currentPath != null && !frame.currentPath.isEmpty()) {
            gc.setStroke(PATH_FUTURE);
            gc.setLineWidth(1.5);
            gc.setLineDashes(4, 4);

            double prevX = frame.roverY * cellW + cellW / 2;
            double prevY = frame.roverX * cellH + cellH / 2;
            for (int[] node : frame.currentPath) {
                double px = node[1] * cellW + cellW / 2;
                double py = node[0] * cellH + cellH / 2;
                gc.strokeLine(prevX, prevY, px, py);
                prevX = px;
                prevY = py;
            }
            gc.setLineDashes(null);
        }

        // rover
        double rx = frame.roverY * cellW + cellW / 2;
        double ry = frame.roverX * cellH + cellH / 2;
        double size = Math.max(cellW, cellH);

        // outer glow
        gc.setFill(ROVER_GLOW);
        gc.fillOval(rx - size * 1.2, ry - size * 1.2, size * 2.4, size * 2.4);

        // inner dot
        gc.setFill(ROVER_COLOR);
        gc.fillOval(rx - size * 0.45, ry - size * 0.45, size * 0.9, size * 0.9);

        // ring
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(rx - size * 0.45, ry - size * 0.45, size * 0.9, size * 0.9);
    }

    // ========== PLAYBACK ==========

    private void togglePlayback() {
        if (isPlaying) stopPlayback(); else startPlayback();
    }

    private void startPlayback() {
        if (currentFrameIndex >= frames.size() - 1) {
            consoleLogArea.clear();
            showFrame(0);
        }
        isPlaying = true;
        playPauseBtn.setText("Pause");

        double intervalMs = 500.0 / getSpeedMultiplier();
        playbackTimeline = new Timeline(new KeyFrame(Duration.millis(intervalMs), e -> {
            if (currentFrameIndex < frames.size() - 1) showFrame(currentFrameIndex + 1);
            else stopPlayback();
        }));
        playbackTimeline.setCycleCount(Animation.INDEFINITE);
        playbackTimeline.play();
    }

    private void stopPlayback() {
        isPlaying = false;
        playPauseBtn.setText("Play");
        if (playbackTimeline != null) playbackTimeline.stop();
    }

    private void updatePlaybackSpeed() {
        if (isPlaying) { stopPlayback(); startPlayback(); }
    }

    private double getSpeedMultiplier() {
        return switch (speedSelector.getValue()) {
            case "0.5x" -> 0.5;
            case "2x" -> 2.0;
            case "5x" -> 5.0;
            case "10x" -> 10.0;
            case "50x" -> 50.0;
            default -> 1.0;
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
