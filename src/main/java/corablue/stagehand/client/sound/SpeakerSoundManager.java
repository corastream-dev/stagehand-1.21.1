package corablue.stagehand.client.sound;

import corablue.stagehand.block.entity.AmbienceSpeakerBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class SpeakerSoundManager {
    private static final Map<BlockPos, SpeakerSoundInstance> PLAYING_SOUNDS = new HashMap<>();

    public static void tickSpeaker(AmbienceSpeakerBlockEntity be) {
        BlockPos pos = be.getPos();
        SpeakerSoundInstance currentInstance = PLAYING_SOUNDS.get(pos);

        boolean redstonePowered = be.getCachedState().get(corablue.stagehand.block.custom.AmbienceSpeakerBlock.POWERED);

        // --- 1. Range Check to Prevent 0-Volume Drops ---
        boolean inRange = false;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            // Calculate distance using the exact same math from SpeakerSoundInstance
            double distance = Math.sqrt(player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            distance -= 1;
            // Only mark as in range if we are strictly inside the volume radius (so volume > 0)
            inRange = distance < be.getRange();
        }

        // The sound should only play if it's toggled on, unpowered, AND the player is close enough to hear it.
        boolean shouldPlay = be.isPlaying() && !redstonePowered && inRange;

        // --- 2. Stop Logic ---
        if (!shouldPlay) {
            if (currentInstance != null) {
                currentInstance.stopInstance();
                PLAYING_SOUNDS.remove(pos);
            }
            return; // Exit early!
        }

        // --- 3. The Zombie Check ---
        if (currentInstance != null) {
            // Failsafe: Check if the sound engine itself killed the sound behind our back
            boolean isDeadInEngine = !MinecraftClient.getInstance().getSoundManager().isPlaying(currentInstance);

            if (currentInstance.isDone() || currentInstance.getSpeaker() != be || currentInstance.getSpeaker().isRemoved() || isDeadInEngine) {
                currentInstance.stopInstance();
                PLAYING_SOUNDS.remove(pos);
                currentInstance = null; // Force a fresh start below
            }
        }

        // --- 4. Start New Sound ---
        if (currentInstance == null) {
            SoundEvent sound = Registries.SOUND_EVENT.get(be.getCurrentSound());
            if (sound != null) {
                SpeakerSoundInstance newInstance = new SpeakerSoundInstance(be, sound);
                MinecraftClient.getInstance().getSoundManager().play(newInstance);
                PLAYING_SOUNDS.put(pos, newInstance);
            }
        }
    }
}