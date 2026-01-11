package org.lxveyanx.trajectorymod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TrajectoryModClient implements ClientModInitializer {

    private static KeyBinding configKey;

    @Override
    public void onInitializeClient() {
        TrajectoryConfig.load();
        TrajectoryRenderer.init();

        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.trajectorymod.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.trajectorymod.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKey.wasPressed()) {
                // ОТКРЫВАЕМ НАШ НОВЫЙ GUI
                client.setScreen(new TrajectoryConfigScreen(client.currentScreen));
            }
        });
    }
}
