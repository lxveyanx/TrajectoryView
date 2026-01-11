package org.lxveyanx.trajectorymod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TrajectoryConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("trajectory_mod.json").toFile();
    private static TrajectoryConfig INSTANCE;

    public boolean enabled = true;

    // Что отображать
    public boolean showTrident = true;
    public boolean showCrossbow = true;
    public boolean showOthers = true;

    public int colorStart = 0x00FFFF; // Бирюзовый
    public int colorEnd = 0xB400FF;   // Фиолетовый

    public int markerColorBlock = 0xC832FF; // Фиолетовый маркер (обычный)
    public int markerColorEntity = 0xFF0000; // Красный маркер (враг)

    // --- Helpers для GUI ---
    // Получение компонента цвета (0-255)
    public static int getRed(int color) { return (color >> 16) & 0xFF; }
    public static int getGreen(int color) { return (color >> 8) & 0xFF; }
    public static int getBlue(int color) { return color & 0xFF; }

    // Сборка цвета обратно
    public static int toHex(int r, int g, int b) {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, TrajectoryConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                INSTANCE = new TrajectoryConfig();
            }
        } else {
            INSTANCE = new TrajectoryConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TrajectoryConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }
}
