package com.pawel.snake3d;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Snake3DApp extends Application {

    private static final int GRID_SIZE = 18;
    private static final double TILE = 42;
    private static final double FLOOR_THICKNESS = 20;
    private static final double SEGMENT_SIZE = 30;
    private static final double WALL_HEIGHT = 36;

    private final Random random = new Random();
    private final LinkedList<Cell> snake = new LinkedList<>();

    private Direction currentDirection = Direction.RIGHT;
    private Direction queuedDirection = Direction.RIGHT;

    private Cell food;

    private boolean started = false;
    private boolean paused = false;
    private boolean gameOver = false;

    private int score = 0;
    private int bestScore = 0;

    private final Group world = new Group();
    private final Group arenaGroup = new Group();
    private final Group snakeGroup = new Group();
    private final Group foodGroup = new Group();
    private final Group decoGroup = new Group();

    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate cameraRotateX = new Rotate(-33, Rotate.X_AXIS);
    private final Rotate cameraRotateY = new Rotate(-44, Rotate.Y_AXIS);
    private final Translate cameraPosition = new Translate(0, -290, -1020);

    private final Rotate worldRotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate worldFloat = new Translate(0, 0, 0);

    private Timeline gameLoop;
    private AnimationTimer renderLoop;

    private final Label scoreValue = new Label("0");
    private final Label bestValue = new Label("0");
    private final Label speedValue = new Label("1");
    private final Label centerTitle = new Label("SNAKE 3D");
    private final Label centerSubtitle = new Label("SPACE — start  •  WASD / strzałki — ruch  •  P — pauza");

    private Sphere foodOrb;
    private PointLight foodLight;
    private PointLight movingAccentLight;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        buildWorld();
        buildSnake();
        spawnFood();
        renderSnake();
        renderFood();

        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        camera.getTransforms().addAll(cameraRotateY, cameraRotateX, cameraPosition);

        SubScene subScene = new SubScene(world, 1280, 820, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.rgb(6, 10, 18));
        subScene.setCamera(camera);

        StackPane root = new StackPane(subScene, buildOverlay());
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #07101c, #0e1628 60%, #0a0f18);");

        Scene scene = new Scene(root, 1280, 820, true);
        registerInput(scene);
        setupLoops();
        updateHud();
        updateCenterMessage();

        stage.setTitle("Snake 3D Deluxe");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.show();

        renderLoop.start();
    }

    private void buildWorld() {
        world.getTransforms().addAll(worldRotateY, worldFloat);
        buildArena();
        buildDecorations();
        buildLights();
        world.getChildren().addAll(arenaGroup, snakeGroup, foodGroup, decoGroup);
    }

    private void buildArena() {
        double arenaSize = GRID_SIZE * TILE;

        Box floor = new Box(arenaSize + 80, FLOOR_THICKNESS, arenaSize + 80);
        floor.setTranslateY(16);
        floor.setMaterial(material("#101a2f", "#20345d", 96));
        arenaGroup.getChildren().add(floor);

        Box innerFloor = new Box(arenaSize + 8, 8, arenaSize + 8);
        innerFloor.setTranslateY(4);
        innerFloor.setMaterial(material("#0d1424", "#2d5f94", 110));
        arenaGroup.getChildren().add(innerFloor);

        for (int i = 0; i <= GRID_SIZE; i++) {
            double linePos = -arenaSize / 2 + i * TILE;

            Box gridLineX = new Box(2, 2, arenaSize);
            gridLineX.setTranslateX(linePos);
            gridLineX.setTranslateY(-1);
            gridLineX.setMaterial(material("#2a4565", "#72d6ff", 64));

            Box gridLineZ = new Box(arenaSize, 2, 2);
            gridLineZ.setTranslateZ(linePos);
            gridLineZ.setTranslateY(-1);
            gridLineZ.setMaterial(material("#2a4565", "#72d6ff", 64));

            arenaGroup.getChildren().addAll(gridLineX, gridLineZ);
        }

        double half = arenaSize / 2 + 24;
        arenaGroup.getChildren().addAll(
                wall(arenaSize + 48, WALL_HEIGHT, 10, 0, -6, -half),
                wall(arenaSize + 48, WALL_HEIGHT, 10, 0, -6, half),
                wall(10, WALL_HEIGHT, arenaSize + 48, -half, -6, 0),
                wall(10, WALL_HEIGHT, arenaSize + 48, half, -6, 0)
        );

        double pillarOffset = arenaSize / 2 + 26;
        arenaGroup.getChildren().addAll(
                pillar(-pillarOffset, -pillarOffset),
                pillar(pillarOffset, -pillarOffset),
                pillar(-pillarOffset, pillarOffset),
                pillar(pillarOffset, pillarOffset)
        );
    }

    private void buildDecorations() {
        for (int i = 0; i < 180; i++) {
            Sphere star = new Sphere(randomRange(1.0, 2.5));
            star.setTranslateX(randomRange(-1600, 1600));
            star.setTranslateY(randomRange(-1200, -200));
            star.setTranslateZ(randomRange(-1600, 1600));
            star.setMaterial(material(randomStarColor(), "#ffffff", 24));
            decoGroup.getChildren().add(star);
        }

        for (int i = 0; i < 24; i++) {
            Sphere orb = new Sphere(randomRange(6, 12));
            orb.setTranslateX(randomRange(-900, 900));
            orb.setTranslateY(randomRange(-220, -40));
            orb.setTranslateZ(randomRange(-900, 900));
            orb.setMaterial(material("#14233f", i % 2 == 0 ? "#6de6ff" : "#8f7bff", 90));
            decoGroup.getChildren().add(orb);
        }
    }

    private void buildLights() {
        AmbientLight ambient = new AmbientLight(Color.color(0.42, 0.50, 0.70));

        PointLight keyLight = new PointLight(Color.web("#9cd8ff"));
        keyLight.setTranslateX(-420);
        keyLight.setTranslateY(-320);
        keyLight.setTranslateZ(-600);

        PointLight rimLight = new PointLight(Color.web("#6fffd4"));
        rimLight.setTranslateX(480);
        rimLight.setTranslateY(-180);
        rimLight.setTranslateZ(420);

        PointLight magentaLight = new PointLight(Color.web("#9f7dff"));
        magentaLight.setTranslateX(-520);
        magentaLight.setTranslateY(-120);
        magentaLight.setTranslateZ(560);

        movingAccentLight = new PointLight(Color.web("#7cf6ff"));
        movingAccentLight.setTranslateY(-80);

        world.getChildren().addAll(ambient, keyLight, rimLight, magentaLight, movingAccentLight);
    }

    private StackPane buildOverlay() {
        Label title = hudLabel("SNAKE 3D", 28, true, "#f5f7ff");
        title.setStyle(title.getStyle() + "-fx-letter-spacing: 2px;");

        HBox hud = new HBox(18,
                statCard("SCORE", scoreValue),
                statCard("BEST", bestValue),
                statCard("SPEED", speedValue)
        );
        hud.setAlignment(Pos.TOP_LEFT);

        VBox topLeft = new VBox(14, title, hud);
        topLeft.setAlignment(Pos.TOP_LEFT);
        topLeft.setPadding(new Insets(24));

        Label controls = hudLabel("WASD / STRZAŁKI • SPACE • P", 14, false, "#a9bad3");
        controls.setStyle(controls.getStyle() +
                "-fx-background-color: rgba(12,18,30,0.54);" +
                "-fx-border-color: rgba(145,205,255,0.22);" +
                "-fx-background-radius: 18;" +
                "-fx-border-radius: 18;" +
                "-fx-padding: 10 16 10 16;");

        StackPane topRight = new StackPane(controls);
        StackPane.setAlignment(topRight, Pos.TOP_RIGHT);
        StackPane.setMargin(topRight, new Insets(28));

        centerTitle.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 40));
        centerTitle.setTextFill(Color.web("#f5f8ff"));
        centerSubtitle.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 15));
        centerSubtitle.setTextFill(Color.web("#afc0d9"));

        VBox centerBox = new VBox(14, centerTitle, centerSubtitle);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMaxWidth(700);
        centerBox.setPadding(new Insets(26, 34, 26, 34));
        centerBox.setStyle(
                "-fx-background-color: rgba(10, 16, 28, 0.74);" +
                "-fx-border-color: rgba(151, 213, 255, 0.24);" +
                "-fx-border-width: 1.2;" +
                "-fx-background-radius: 28;" +
                "-fx-border-radius: 28;"
        );

        StackPane centerWrap = new StackPane(centerBox);
        centerWrap.setPickOnBounds(false);

        StackPane overlay = new StackPane();
        overlay.getChildren().addAll(topLeft, topRight, centerWrap);
        StackPane.setAlignment(topLeft, Pos.TOP_LEFT);
        StackPane.setAlignment(centerWrap, Pos.CENTER);
        overlay.setMouseTransparent(true);
        return overlay;
    }

    private VBox statCard(String name, Label valueLabel) {
        Label nameLabel = hudLabel(name, 12, true, "#90a8c8");
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 24));
        valueLabel.setTextFill(Color.web("#ffffff"));

        VBox box = new VBox(6, nameLabel, valueLabel);
        box.setPadding(new Insets(14, 18, 14, 18));
        box.setPrefWidth(140);
        box.setStyle(
                "-fx-background-color: rgba(14, 22, 37, 0.72);" +
                "-fx-border-color: rgba(115, 190, 255, 0.20);" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 22;" +
                "-fx-border-radius: 22;"
        );
        return box;
    }

    private Label hudLabel(String text, int size, boolean bold, String color) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.MEDIUM, size));
        label.setTextFill(Color.web(color));
        return label;
    }

    private void registerInput(Scene scene) {
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();

            if (code == KeyCode.SPACE) {
                if (!started || gameOver) {
                    restartGame();
                    started = true;
                    paused = false;
                    gameOver = false;
                    gameLoop.play();
                    updateCenterMessage();
                }
                return;
            }

            if (code == KeyCode.P && started && !gameOver) {
                paused = !paused;
                if (paused) {
                    gameLoop.pause();
                } else {
                    gameLoop.play();
                }
                updateCenterMessage();
                return;
            }

            if (code == KeyCode.R) {
                restartGame();
                started = true;
                paused = false;
                gameOver = false;
                gameLoop.play();
                updateCenterMessage();
                return;
            }

            Direction newDirection = null;
            if (code == KeyCode.W || code == KeyCode.UP) newDirection = Direction.UP;
            if (code == KeyCode.S || code == KeyCode.DOWN) newDirection = Direction.DOWN;
            if (code == KeyCode.A || code == KeyCode.LEFT) newDirection = Direction.LEFT;
            if (code == KeyCode.D || code == KeyCode.RIGHT) newDirection = Direction.RIGHT;

            if (newDirection != null && !isOpposite(currentDirection, newDirection)) {
                queuedDirection = newDirection;
                if (!started) {
                    started = true;
                    paused = false;
                    gameOver = false;
                    gameLoop.play();
                    updateCenterMessage();
                }
            }
        });
    }

    private void setupLoops() {
        rebuildGameLoop();

        renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = now / 1_000_000_000.0;

                worldRotateY.setAngle(Math.sin(t * 0.45) * 5.5);
                worldFloat.setY(Math.sin(t * 1.2) * 5.5);

                cameraRotateY.setAngle(-44 + Math.sin(t * 0.30) * 4.0);
                cameraRotateX.setAngle(-33 + Math.cos(t * 0.50) * 1.6);
                cameraPosition.setY(-290 + Math.sin(t * 0.9) * 10);
                cameraPosition.setZ(-1020 + Math.cos(t * 0.55) * 18);

                if (foodOrb != null) {
                    foodOrb.setTranslateY(-16 + Math.sin(t * 3.4) * 6.5);
                    foodOrb.setRotate(foodOrb.getRotate() + 1.7);
                }

                if (foodLight != null && food != null) {
                    foodLight.setTranslateX(toWorld(food.x()));
                    foodLight.setTranslateY(-38 + Math.sin(t * 3.2) * 7);
                    foodLight.setTranslateZ(toWorld(food.z()));
                }

                if (!snake.isEmpty()) {
                    Cell head = snake.getFirst();
                    movingAccentLight.setTranslateX(toWorld(head.x()));
                    movingAccentLight.setTranslateZ(toWorld(head.z()));
                    movingAccentLight.setTranslateY(-55 + Math.cos(t * 4.0) * 8);
                }
            }
        };
    }

    private void rebuildGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        gameLoop = new Timeline(new KeyFrame(Duration.millis(currentTickMs()), e -> stepGame()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
    }

    private double currentTickMs() {
        int growth = Math.max(0, snake.size() - 4);
        return Math.max(78, 220 - growth * 7.5);
    }

    private void restartGame() {
        snake.clear();
        snake.add(new Cell(7, 9));
        snake.add(new Cell(6, 9));
        snake.add(new Cell(5, 9));
        snake.add(new Cell(4, 9));

        currentDirection = Direction.RIGHT;
        queuedDirection = Direction.RIGHT;
        score = 0;
        gameOver = false;
        paused = false;
        spawnFood();
        renderSnake();
        renderFood();
        rebuildGameLoop();
        updateHud();
    }

    private void buildSnake() {
        if (snake.isEmpty()) {
            snake.add(new Cell(7, 9));
            snake.add(new Cell(6, 9));
            snake.add(new Cell(5, 9));
            snake.add(new Cell(4, 9));
        }
    }

    private void stepGame() {
        if (!started || paused || gameOver) {
            return;
        }

        if (!isOpposite(currentDirection, queuedDirection)) {
            currentDirection = queuedDirection;
        }

        Cell head = snake.getFirst();
        Cell next = switch (currentDirection) {
            case UP -> new Cell(head.x(), head.z() - 1);
            case DOWN -> new Cell(head.x(), head.z() + 1);
            case LEFT -> new Cell(head.x() - 1, head.z());
            case RIGHT -> new Cell(head.x() + 1, head.z());
        };

        if (next.x() < 0 || next.x() >= GRID_SIZE || next.z() < 0 || next.z() >= GRID_SIZE) {
            triggerGameOver();
            return;
        }

        boolean grow = next.equals(food);
        List<Cell> collisionList = grow ? snake : snake.subList(0, snake.size() - 1);
        if (collisionList.contains(next)) {
            triggerGameOver();
            return;
        }

        snake.addFirst(next);
        if (!grow) {
            snake.removeLast();
        } else {
            score += 10;
            bestScore = Math.max(bestScore, score);
            spawnFood();
            rebuildGameLoop();
            gameLoop.play();
        }

        renderSnake();
        renderFood();
        updateHud();
    }

    private void triggerGameOver() {
        gameOver = true;
        bestScore = Math.max(bestScore, score);
        gameLoop.stop();
        updateHud();
        updateCenterMessage();
    }

    private void spawnFood() {
        Cell candidate;
        do {
            candidate = new Cell(random.nextInt(GRID_SIZE), random.nextInt(GRID_SIZE));
        } while (snake.contains(candidate));
        food = candidate;
    }

    private void renderSnake() {
        snakeGroup.getChildren().clear();

        for (int i = snake.size() - 1; i >= 0; i--) {
            Cell cell = snake.get(i);
            boolean isHead = i == 0;
            double size = isHead ? SEGMENT_SIZE + 3 : SEGMENT_SIZE;

            Box cube = new Box(size, size, size);
            cube.setTranslateX(toWorld(cell.x()));
            cube.setTranslateY(-10);
            cube.setTranslateZ(toWorld(cell.z()));
            cube.setCullFace(CullFace.BACK);

            if (isHead) {
                cube.setMaterial(material("#9cffea", "#2affb9", 128));

                Sphere headAura = new Sphere(24);
                headAura.setTranslateX(cube.getTranslateX());
                headAura.setTranslateY(-10);
                headAura.setTranslateZ(cube.getTranslateZ());
                headAura.setMaterial(material("#1b3644", "#6effdf", 96));
                headAura.setOpacity(0.22);
                snakeGroup.getChildren().add(headAura);
            } else {
                double blend = 0.25 + (snake.size() - i) * 0.04;
                cube.setMaterial(material(mixColor(blend), "#79ffe2", 90));
            }

            snakeGroup.getChildren().add(cube);
        }
    }

    private void renderFood() {
        foodGroup.getChildren().clear();

        foodOrb = new Sphere(14);
        foodOrb.setTranslateX(toWorld(food.x()));
        foodOrb.setTranslateY(-16);
        foodOrb.setTranslateZ(toWorld(food.z()));
        foodOrb.setMaterial(material("#ff7aa8", "#fffbff", 150));

        Sphere halo = new Sphere(23);
        halo.setTranslateX(toWorld(food.x()));
        halo.setTranslateY(-16);
        halo.setTranslateZ(toWorld(food.z()));
        halo.setMaterial(material("#281326", "#ff9cc3", 110));
        halo.setOpacity(0.22);
        halo.setDrawMode(DrawMode.FILL);

        foodLight = new PointLight(Color.web("#ff84b1"));
        foodLight.setTranslateX(toWorld(food.x()));
        foodLight.setTranslateY(-32);
        foodLight.setTranslateZ(toWorld(food.z()));

        foodGroup.getChildren().addAll(halo, foodOrb, foodLight);
    }

    private void updateHud() {
        scoreValue.setText(String.valueOf(score));
        bestValue.setText(String.valueOf(bestScore));
        speedValue.setText(String.valueOf(Math.max(1, (snake.size() - 1) / 2)));
    }

    private void updateCenterMessage() {
        if (!started && !gameOver) {
            centerTitle.setText("SNAKE 3D");
            centerSubtitle.setText("SPACE — start  •  WASD / strzałki — ruch  •  P — pauza");
            centerTitle.setVisible(true);
            centerSubtitle.setVisible(true);
            return;
        }

        if (paused) {
            centerTitle.setText("PAUZA");
            centerSubtitle.setText("P — wróć do gry  •  R — restart");
            centerTitle.setVisible(true);
            centerSubtitle.setVisible(true);
            return;
        }

        if (gameOver) {
            centerTitle.setText("GAME OVER");
            centerSubtitle.setText("Wynik: " + score + "  •  SPACE / R — restart");
            centerTitle.setVisible(true);
            centerSubtitle.setVisible(true);
            return;
        }

        centerTitle.setVisible(false);
        centerSubtitle.setVisible(false);
    }

    private Box wall(double w, double h, double d, double x, double y, double z) {
        Box wall = new Box(w, h, d);
        wall.setTranslateX(x);
        wall.setTranslateY(y);
        wall.setTranslateZ(z);
        wall.setMaterial(material("#15243f", "#6eddff", 100));
        return wall;
    }

    private Group pillar(double x, double z) {
        Group group = new Group();

        Box column = new Box(18, 90, 18);
        column.setTranslateX(x);
        column.setTranslateY(-28);
        column.setTranslateZ(z);
        column.setMaterial(material("#192947", "#77d7ff", 120));

        Sphere cap = new Sphere(10);
        cap.setTranslateX(x);
        cap.setTranslateY(-78);
        cap.setTranslateZ(z);
        cap.setMaterial(material("#8de8ff", "#ffffff", 180));

        PointLight light = new PointLight(Color.web("#8ae6ff"));
        light.setTranslateX(x);
        light.setTranslateY(-82);
        light.setTranslateZ(z);

        group.getChildren().addAll(column, cap, light);
        return group;
    }

    private PhongMaterial material(String diffuseHex, String specularHex, double power) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.web(diffuseHex));
        material.setSpecularColor(Color.web(specularHex));
        material.setSpecularPower(power);
        return material;
    }

    private double toWorld(int grid) {
        return (grid - (GRID_SIZE - 1) / 2.0) * TILE;
    }

    private boolean isOpposite(Direction a, Direction b) {
        return (a == Direction.UP && b == Direction.DOWN)
                || (a == Direction.DOWN && b == Direction.UP)
                || (a == Direction.LEFT && b == Direction.RIGHT)
                || (a == Direction.RIGHT && b == Direction.LEFT);
    }

    private double randomRange(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private String randomStarColor() {
        String[] colors = {
                "#8ab4ff", "#b7f1ff", "#8fffe1", "#d4c1ff", "#ffffff"
        };
        return colors[random.nextInt(colors.length)];
    }

    private String mixColor(double blend) {
        blend = Math.max(0.0, Math.min(1.0, blend));

        int r1 = 29, g1 = 84, b1 = 86;
        int r2 = 96, g2 = 255, b2 = 217;

        int r = (int) Math.round(r1 + (r2 - r1) * blend);
        int g = (int) Math.round(g1 + (g2 - g1) * blend);
        int b = (int) Math.round(b1 + (b2 - b1) * blend);

        return String.format("#%02x%02x%02x", r, g, b);
    }
}
