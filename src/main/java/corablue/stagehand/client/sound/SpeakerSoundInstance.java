package corablue.stagehand.client.sound;

import corablue.stagehand.block.entity.AmbienceSpeakerBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

@Environment(EnvType.CLIENT)
public class SpeakerSoundInstance extends MovingSoundInstance {
    private final AmbienceSpeakerBlockEntity speaker;
    private final int initialRange;

    public SpeakerSoundInstance(AmbienceSpeakerBlockEntity speaker, SoundEvent sound) {
        super(sound, SoundCategory.AMBIENT, Random.create());
        this.speaker = speaker;
        this.initialRange = speaker.getRange();
        this.repeat = true;
        this.repeatDelay = 0;
        this.x = speaker.getPos().getX() + 0.5;
        this.y = speaker.getPos().getY() + 0.5;
        this.z = speaker.getPos().getZ() + 0.5;
        updateVolume();
    }

    // This is the ad-hoc method needed for the Manager to kill the sound
    public void stopInstance() {
        this.setDone();
    }

    public AmbienceSpeakerBlockEntity getSpeaker() {
        return this.speaker;
    }

    @Override
    public void tick() {
        // Check redstone power from the block state
        boolean isPowered = this.speaker.getCachedState().get(corablue.stagehand.block.custom.AmbienceSpeakerBlock.POWERED);

        // Kill the instance if the block is broken, toggled off, sound changed,
        // range changed, OR powered by redstone
        if (this.speaker.isRemoved() ||
                !this.speaker.isPlaying() ||
                isPowered ||
                !this.speaker.getCurrentSound().equals(this.getId()) ||
                this.speaker.getRange() != this.initialRange) {

            this.stopInstance();
        } else {
            updateVolume();
        }
    }

    private void updateVolume() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            this.volume = 0f;
            return;
        }

        // 1. Calculate the exact distance from the player to the speaker
        double distance = Math.sqrt(player.squaredDistanceTo(this.x, this.y, this.z));
        distance -= 1;

        // 2. Define our custom fade boundaries
        double fadeStart = 1.0; // Stay at 100% volume up to 1 block away
        double fadeEnd = this.initialRange; // Hit 0% volume exactly at this block distance

        if (distance <= fadeStart) {
            // Player is inside the 1-block bubble.
            // (We keep the range/16.0f math so ranges like 32 or 64 still scale up OpenAL's 3D panning)
            this.volume = Math.max(1.0f, this.initialRange / 16.0f);
        } else if (distance >= fadeEnd) {
            // Player crossed the boundary. Force absolute silence!
            this.volume = 0.0f;
        } else {
            // Player is walking away. Interpolate the volume down to 0 perfectly.
            float distancePercent = (float) ((distance - fadeStart) / (fadeEnd - fadeStart));
            float rawVolume = 1.0f - distancePercent;

            float maxVol = Math.max(1.0f, this.initialRange / 16.0f);
            this.volume = rawVolume * maxVol;
        }
    }
}