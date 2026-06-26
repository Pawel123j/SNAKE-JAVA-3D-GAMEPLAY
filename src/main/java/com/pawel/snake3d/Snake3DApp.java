package com.pawel.snake3d;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Snake3DApp extends Application {

    private static final int GRID_SIZE = 18;
    private static final double TILE = 42;
    private static final double FLOOR_THICKNESS = 20;
    private static final double SEGMENT_SIZE = 30;
    private static final double WALL_HEIGHT = 36;

    private final Random random = new Random();

    private final SnakeGame game = new SnakeGame(GRID_SIZE, random);
    private final HighScoreStore highScoreStore = new HighScoreStore();
    private final Leaderboard leaderboard = new Leaderboard();
    private final Settings settings = new Settings();
    private final SoundManager sound = new SoundManager();
    private int bestScore;

    private final Group world = new Group();
    private final Group arenaGroup = new Group();
    private final Group snakeGroup = new Group();
    private final Group foodGroup = new Group();
    private final Group decoGroup = new Group();
    private final Group particleGroup = new Group();
    private final Group obstacleGroup = new Group();

    /** Reused snake segment nodes — grown on demand, never recreated per frame. */
    private final List<Box> segmentPool = new ArrayList<>();
    private Sphere headAura;

    /** Snapshots used to interpolate the snake smoothly between game ticks. */
    private List<Cell> currentBody = List.of();
    private List<Cell> previousBody = List.of();
    private long stepTimeNanos;
    private double stepDurationNanos = 1;

    /** Cache of immutable materials, keyed by their colour/power signature. */
    private final Map<String, PhongMaterial> materialCache = new HashMap<>();

    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate cameraRotateX = new Rotate(-33, Rotate.X_AXIS);
    private final Rotate cameraRotateY = new Rotate(-44, Rotate.Y_AXIS);
    private final Translate cameraPosition = new Translate(0, -290, -1020);

    private final Rotate worldRotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate worldFloat = new Translate(0, 0, 0);
    private final Translate shake = new Translate(0, 0, 0);

    private Timeline gameLoop;
    private AnimationTimer renderLoop;

    private final Label scoreValue = new Label("0");
    private final Label bestValue = new Label("0");
    private final Label speedValue = new Label("1");
    private final Label difficultyValue = new Label(Difficulty.NORMAL.label());
    private final Label boardValue = new Label(GameMode.CLASSIC.label());
    private final Label comboValue = new Label("x1");
    private final Label centerTitle = new Label("SNAKE 3D");
    private final Label centerSubtitle = new Label("SPACE — start  •  WASD / strzałki — ruch  •  P — pauza");
    private final Label centerScores = new Label();

    private Sphere foodOrb;
    private Sphere foodHalo;
    private PointLight foodLight;
    private Sphere bonusOrb;
    private Sphere bonusHalo;
    private PointLight bonusLight;
    private PointLight movingAccentLight;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        bestScore = Math.max(highScoreStore.load(), leaderboard.best());
        game.setDifficulty(settings.difficulty());
        game.setMode(settings.mode());
        sound.setEnabled(settings.isSoundEnabled());

        buildWorld();
        renderObstacles();
        resetInterpolation();
        syncSnakeNodes();
        positionFood();

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
        stage.setOnHidden(e -> sound.shutdown());
        stage.show();

        renderLoop.start();
    }

    private void buildWorld() {
        world.getTransforms().addAll(worldRotateY, worldFloat, shake);
        buildArena();
        buildDecorations();
        buildLights();
        buildFoodNodes();
        world.getChildren().addAll(arenaGroup, obstacleGroup, snakeGroup, foodGroup, decoGroup, particleGroup);
    }

    /** Rebuilds obstacle pylons from the model (changes only on reset / mode switch). */
    private void renderObstacles() {
        obstacleGroup.getChildren().clear();
        for (Cell cell : game.obstacles()) {
            Box pylon = new Box(30, 54, 30);
            pylon.setTranslateX(toWorld(cell.x()));
            pylon.setTranslateY(-22);
            pylon.setTranslateZ(toWorld(cell.z()));
            pylon.setMaterial(material("#3b2150", "#ff7ad0", 70));
            obstacleGroup.getChildren().add(pylon);
        }
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

    /** Builds the persistent food / bonus / aura nodes once; positions update each frame. */
    private void buildFoodNodes() {
        foodOrb = new Sphere(14);
        foodOrb.setMaterial(material("#ff7aa8", "#fffbff", 150));

        foodHalo = new Sphere(23);
        foodHalo.setMaterial(material("#281326", "#ff9cc3", 110));
        foodHalo.setOpacity(0.22);
        foodHalo.setDrawMode(DrawMode.FILL);

        foodLight = new PointLight(Color.web("#ff84b1"));

        bonusOrb = new Sphere(16);
        bonusOrb.setMaterial(material("#ffd451", "#fff7d6", 160));

        bonusHalo = new Sphere(28);
        bonusHalo.setMaterial(material("#3a2c0c", "#ffe690", 120));
        bonusHalo.setOpacity(0.25);

        bonusLight = new PointLight(Color.web("#ffd451"));

        foodGroup.getChildren().addAll(foodHalo, foodOrb, foodLight, bonusHalo, bonusOrb, bonusLight);

        headAura = new Sphere(24);
        headAura.setMaterial(material("#1b3644", "#6effdf", 96));
        headAura.setOpacity(0.22);
    }

    private StackPane buildOverlay() {
        Label title = hudLabel("SNAKE 3D", 28, true, "#f5f7ff");
        title.setStyle(title.getStyle() + "-fx-letter-spacing: 2px;");

        HBox hud = new HBox(14,
                statCard("SCORE", scoreValue),
                statCard("BEST", bestValue),
                statCard("COMBO", comboValue),
                statCard("SPEED", speedValue),
                statCard("POZIOM", difficultyValue),
                statCard("PLANSZA", boardValue)
        );
        hud.setAlignment(Pos.TOP_LEFT);

        VBox topLeft = new VBox(14, title, hud);
        topLeft.setAlignment(Pos.TOP_LEFT);
        topLeft.setPadding(new Insets(24));

        Label controls = hudLabel("WASD • SPACE • P • 1·2·3 POZIOM • B PLANSZA • M DŹWIĘK", 13, false, "#a9bad3");
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
        centerScores.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        centerScores.setTextFill(Color.web("#ffd451"));

        VBox centerBox = new VBox(14, centerTitle, centerSubtitle, centerScores);
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
        box.setPadding(new Insets(13, 16, 13, 16));
        box.setPrefWidth(122);
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

            switch (code) {
                case SPACE, R -> {
                    startOrRestart();
                    return;
                }
                case P -> {
                    if (game.state() == GameState.RUNNING || game.state() == GameState.PAUSED) {
                        game.togglePause();
                        syncLoopWithState();
                        updateCenterMessage();
                    }
                    return;
                }
                case M -> {
                    settings.setSoundEnabled(sound.toggleMute());
                    return;
                }
                case B -> {
                    cycleMode();
                    return;
                }
                case DIGIT1 -> {
                    selectDifficulty(Difficulty.EASY);
                    return;
                }
                case DIGIT2 -> {
                    selectDifficulty(Difficulty.NORMAL);
                    return;
                }
                case DIGIT3 -> {
                    selectDifficulty(Difficulty.HARD);
                    return;
                }
                default -> { /* fall through to movement keys */ }
            }

            Direction newDirection = directionFor(code);
            if (newDirection != null) {
                game.setDirection(newDirection);
                if (game.state() == GameState.READY) {
                    game.start();
                    resetInterpolation();
                    rebuildGameLoop();
                    gameLoop.play();
                    updateCenterMessage();
                }
            }
        });
    }

    private Direction directionFor(KeyCode code) {
        return switch (code) {
            case W, UP -> Direction.UP;
            case S, DOWN -> Direction.DOWN;
            case A, LEFT -> Direction.LEFT;
            case D, RIGHT -> Direction.RIGHT;
            default -> null;
        };
    }

    private void startOrRestart() {
        game.start();
        renderObstacles();
        resetInterpolation();
        syncSnakeNodes();
        positionFood();
        rebuildGameLoop();
        gameLoop.play();
        updateHud();
        updateCenterMessage();
    }

    private void selectDifficulty(Difficulty difficulty) {
        game.setDifficulty(difficulty);
        settings.setDifficulty(difficulty);
        if (game.state() == GameState.RUNNING) {
            rebuildGameLoop();
            gameLoop.play();
        }
        updateHud();
    }

    /** Cycles the board mode, persists it and shows the freshly laid-out board. */
    private void cycleMode() {
        GameMode mode = game.mode().next();
        game.setMode(mode); // resets the game to READY with the new layout
        settings.setMode(mode);
        if (gameLoop != null) {
            gameLoop.stop();
        }
        renderObstacles();
        resetInterpolation();
        syncSnakeNodes();
        positionFood();
        updateHud();
        updateCenterMessage();
    }

    private void syncLoopWithState() {
        if (game.state() == GameState.RUNNING) {
            gameLoop.play();
        } else {
            gameLoop.pause();
        }
    }

    /** Aligns the interpolation snapshots with the model's current state. */
    private void resetInterpolation() {
        currentBody = List.copyOf(game.snakeBody());
        previousBody = currentBody;
        stepTimeNanos = System.nanoTime();
        stepDurationNanos = Math.max(1, game.tickMs() * 1_000_000.0);
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

                double alpha = game.state() == GameState.RUNNING
                        ? clamp((now - stepTimeNanos) / stepDurationNanos, 0, 1)
                        : 1.0;
                positionSnake(alpha, t);

                animateFood(t);
            }
        };
    }

    private void animateFood(double t) {
        Cell food = game.food();
        boolean hasFood = food != null;
        foodOrb.setVisible(hasFood);
        foodHalo.setVisible(hasFood);
        foodLight.setLightOn(hasFood);
        if (hasFood) {
            foodOrb.setTranslateY(-16 + Math.sin(t * 3.4) * 6.5);
            foodOrb.setRotate(foodOrb.getRotate() + 1.7);
            foodLight.setTranslateY(-38 + Math.sin(t * 3.2) * 7);
        }

        Cell bonus = game.bonusFood();
        boolean hasBonus = bonus != null;
        bonusOrb.setVisible(hasBonus);
        bonusHalo.setVisible(hasBonus);
        bonusLight.setLightOn(hasBonus);
        if (hasBonus) {
            double pulse = 1.0 + Math.sin(t * 6.0) * 0.18;
            bonusOrb.setScaleX(pulse);
            bonusOrb.setScaleY(pulse);
            bonusOrb.setScaleZ(pulse);
            bonusOrb.setTranslateY(-18 + Math.sin(t * 4.2) * 7);
            bonusOrb.setRotate(bonusOrb.getRotate() + 2.6);
            bonusLight.setTranslateY(-40 + Math.sin(t * 4.0) * 8);
        }
    }

    private void rebuildGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        gameLoop = new Timeline(new KeyFrame(Duration.millis(game.tickMs()), e -> stepGame()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
    }

    private void stepGame() {
        SnakeGame.StepResult result = game.step();

        previousBody = currentBody;
        currentBody = List.copyOf(game.snakeBody());
        stepTimeNanos = System.nanoTime();
        stepDurationNanos = Math.max(1, game.tickMs() * 1_000_000.0);
        syncSnakeNodes();

        Cell head = currentBody.get(0);
        double headX = toWorld(head.x());
        double headZ = toWorld(head.z());

        switch (result) {
            case ATE -> {
                sound.eat();
                refreshBestScore();
                spawnBurst(headX, headZ, "#ff7aa8", "#fffbff");
                rebuildGameLoop();
                gameLoop.play();
            }
            case ATE_BONUS -> {
                sound.bonus();
                refreshBestScore();
                spawnBurst(headX, headZ, "#ffd451", "#fff7d6");
            }
            case WON -> {
                sound.win();
                recordResult();
                spawnBurst(headX, headZ, "#9cffea", "#ffffff");
                gameLoop.stop();
            }
            case GAME_OVER -> {
                sound.gameOver();
                recordResult();
                triggerShake();
                gameLoop.stop();
            }
            default -> { /* MOVED / IGNORED — nothing extra */ }
        }

        positionFood();
        updateHud();
        updateCenterMessage();
    }

    private void refreshBestScore() {
        if (game.score() > bestScore) {
            bestScore = game.score();
            highScoreStore.save(bestScore);
        }
    }

    /** Persists the finished game's score to the best-store and the leaderboard. */
    private void recordResult() {
        refreshBestScore();
        leaderboard.submit(game.score());
    }

    /** Ensures the segment pool and scene-graph children match the snake length. */
    private void syncSnakeNodes() {
        int n = currentBody.size();

        while (segmentPool.size() < n) {
            Box cube = new Box(SEGMENT_SIZE, SEGMENT_SIZE, SEGMENT_SIZE);
            cube.setCullFace(CullFace.BACK);
            segmentPool.add(cube);
        }

        snakeGroup.getChildren().clear();
        snakeGroup.getChildren().add(headAura);

        for (int i = 0; i < n; i++) {
            boolean isHead = i == 0;
            double size = isHead ? SEGMENT_SIZE + 3 : SEGMENT_SIZE;

            Box cube = segmentPool.get(i);
            cube.setWidth(size);
            cube.setHeight(size);
            cube.setDepth(size);

            if (isHead) {
                cube.setMaterial(material("#9cffea", "#2affb9", 128));
            } else {
                double blend = 0.25 + (n - i) * 0.04;
                cube.setMaterial(material(mixColor(blend), "#79ffe2", 90));
            }

            snakeGroup.getChildren().add(cube);
        }
    }

    /** Positions snake segments by interpolating between the last two ticks. */
    private void positionSnake(double alpha, double t) {
        int n = currentBody.size();
        for (int i = 0; i < n; i++) {
            Cell curr = currentBody.get(i);
            Cell prev = i < previousBody.size() ? previousBody.get(i) : curr;

            double x = lerp(toWorld(prev.x()), toWorld(curr.x()), alpha);
            double z = lerp(toWorld(prev.z()), toWorld(curr.z()), alpha);

            Box cube = segmentPool.get(i);
            cube.setTranslateX(x);
            cube.setTranslateY(-10);
            cube.setTranslateZ(z);

            if (i == 0) {
                headAura.setTranslateX(x);
                headAura.setTranslateY(-10);
                headAura.setTranslateZ(z);
                movingAccentLight.setTranslateX(x);
                movingAccentLight.setTranslateZ(z);
                movingAccentLight.setTranslateY(-55 + Math.cos(t * 4.0) * 8);
            }
        }
    }

    /** Places food / bonus orbs over their grid cells (animation handled in the render loop). */
    private void positionFood() {
        Cell food = game.food();
        if (food != null) {
            double x = toWorld(food.x());
            double z = toWorld(food.z());
            foodOrb.setTranslateX(x);
            foodOrb.setTranslateZ(z);
            foodHalo.setTranslateX(x);
            foodHalo.setTranslateY(-16);
            foodHalo.setTranslateZ(z);
            foodLight.setTranslateX(x);
            foodLight.setTranslateZ(z);
        }

        Cell bonus = game.bonusFood();
        if (bonus != null) {
            double x = toWorld(bonus.x());
            double z = toWorld(bonus.z());
            bonusOrb.setTranslateX(x);
            bonusOrb.setTranslateZ(z);
            bonusHalo.setTranslateX(x);
            bonusHalo.setTranslateY(-18);
            bonusHalo.setTranslateZ(z);
            bonusLight.setTranslateX(x);
            bonusLight.setTranslateZ(z);
        }
    }

    /** A short burst of particles where food was eaten. */
    private void spawnBurst(double x, double z, String core, String spec) {
        for (int i = 0; i < 11; i++) {
            Sphere particle = new Sphere(randomRange(2.5, 4.5));
            particle.setMaterial(material(core, spec, 80));
            particle.setTranslateX(x);
            particle.setTranslateY(-16);
            particle.setTranslateZ(z);
            particleGroup.getChildren().add(particle);

            double angle = randomRange(0, Math.PI * 2);
            double radius = randomRange(28, 64);
            TranslateTransition move = new TranslateTransition(Duration.millis(520), particle);
            move.setByX(Math.cos(angle) * radius);
            move.setByZ(Math.sin(angle) * radius);
            move.setByY(-randomRange(20, 58));
            move.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fade = new FadeTransition(Duration.millis(520), particle);
            fade.setFromValue(0.95);
            fade.setToValue(0.0);

            ScaleTransition shrink = new ScaleTransition(Duration.millis(520), particle);
            shrink.setToX(0.2);
            shrink.setToY(0.2);
            shrink.setToZ(0.2);

            ParallelTransition anim = new ParallelTransition(move, fade, shrink);
            anim.setOnFinished(e -> particleGroup.getChildren().remove(particle));
            anim.play();
        }
    }

    /** Brief camera-relative jolt when the snake crashes. */
    private void triggerShake() {
        Timeline shakeTimeline = new Timeline();
        int frames = 8;
        double frameMs = 45;
        for (int i = 0; i <= frames; i++) {
            double amp = 18 * (1 - i / (double) frames);
            double sx = i == frames ? 0 : randomRange(-amp, amp);
            double sz = i == frames ? 0 : randomRange(-amp, amp);
            shakeTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(frameMs * i),
                    new KeyValue(shake.xProperty(), sx),
                    new KeyValue(shake.zProperty(), sz)));
        }
        shakeTimeline.play();
    }

    private void updateHud() {
        scoreValue.setText(String.valueOf(game.score()));
        bestValue.setText(String.valueOf(bestScore));
        comboValue.setText("x" + game.comboMultiplier());
        comboValue.setTextFill(game.comboMultiplier() > 1 ? Color.web("#ffd451") : Color.web("#ffffff"));
        speedValue.setText(String.valueOf(Math.max(1, (game.length() - 1) / 2)));
        difficultyValue.setText(game.difficulty().label());
        boardValue.setText(game.mode().label());
    }

    private void updateCenterMessage() {
        switch (game.state()) {
            case READY -> showCenter("SNAKE 3D",
                    "SPACE — start  •  WASD / strzałki — ruch  •  P — pauza", true);
            case PAUSED -> showCenter("PAUZA", "P — wróć do gry  •  R — restart", false);
            case GAME_OVER -> showCenter("GAME OVER",
                    "Wynik: " + game.score() + "  •  SPACE / R — restart", true);
            case WON -> showCenter("WYGRANA!",
                    "Wypełniłeś arenę! Wynik: " + game.score() + "  •  SPACE / R — restart", true);
            case RUNNING -> {
                centerTitle.setVisible(false);
                centerSubtitle.setVisible(false);
                centerScores.setVisible(false);
            }
        }
    }

    private void showCenter(String title, String subtitle, boolean withScores) {
        centerTitle.setText(title);
        centerSubtitle.setText(subtitle);
        centerTitle.setVisible(true);
        centerSubtitle.setVisible(true);

        centerScores.setText(topScoresText());
        centerScores.setVisible(withScores && !leaderboard.top().isEmpty());
        centerScores.setManaged(centerScores.isVisible());
    }

    private String topScoresText() {
        List<Integer> top = leaderboard.top();
        if (top.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("REKORDY:  ");
        for (int i = 0; i < top.size(); i++) {
            if (i > 0) {
                sb.append("   ·   ");
            }
            sb.append(i + 1).append(". ").append(top.get(i));
        }
        return sb.toString();
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

    /** Returns a cached material; materials are immutable once created. */
    private PhongMaterial material(String diffuseHex, String specularHex, double power) {
        String key = diffuseHex + '|' + specularHex + '|' + power;
        return materialCache.computeIfAbsent(key, k -> {
            PhongMaterial m = new PhongMaterial();
            m.setDiffuseColor(Color.web(diffuseHex));
            m.setSpecularColor(Color.web(specularHex));
            m.setSpecularPower(power);
            return m;
        });
    }

    private double toWorld(int grid) {
        return (grid - (GRID_SIZE - 1) / 2.0) * TILE;
    }

    private static double lerp(double a, double b, double alpha) {
        return a + (b - a) * alpha;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
