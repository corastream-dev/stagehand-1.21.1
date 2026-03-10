package corablue.stagehand.client.sound;

import corablue.stagehand.block.AmbienceSpeakerBlock;
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
        boolean redstonePowered = be.getCachedState().get(AmbienceSpeakerBlock.POWERED);
        boolean inRange = false;
        PlayerEntity player = MinecraftClient.getInstance().player;

        if (player != null) {
            double distance = Math.sqrt(player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            distance -= 1;
            inRange = distance < be.getRange();
        }

        //Speaker plays by default, redstone STOPS it
        //Players just want to place this, no need to do the opposite
        //Contraptions by definition already have redstone to use
        boolean shouldPlay = be.isPlaying() && !redstonePowered && inRange;

        if (!shouldPlay) {
            if (currentInstance != null) {
                currentInstance.stopInstance();
                PLAYING_SOUNDS.remove(pos);
            }
            return;
        }

        if (currentInstance != null) {
            boolean isDeadInEngine = !MinecraftClient.getInstance().getSoundManager().isPlaying(currentInstance);

            if (currentInstance.isDone() || currentInstance.getSpeaker() != be || currentInstance.getSpeaker().isRemoved() || isDeadInEngine) {
                currentInstance.stopInstance();
                PLAYING_SOUNDS.remove(pos);
                currentInstance = null;
            }
        }

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