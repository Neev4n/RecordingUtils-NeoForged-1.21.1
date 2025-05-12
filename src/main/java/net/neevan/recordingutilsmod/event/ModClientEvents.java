package net.neevan.recordingutilsmod.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.level.GameType;
import net.neevan.recordingutilsmod.keybinding.ModKeyBindings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "recordingutilsmod", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)

public class ModClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (ModKeyBindings.SPECTATOR_TOGGLE.consumeClick()) {
            if (mc.gameMode != null) {
                if(mc.gameMode.getPlayerMode() == GameType.CREATIVE){
                    mc.gameMode.setLocalMode(GameType.SPECTATOR);
                } else {
                    mc.gameMode.setLocalMode(GameType.CREATIVE);
                }
            }

        }
    }
}
