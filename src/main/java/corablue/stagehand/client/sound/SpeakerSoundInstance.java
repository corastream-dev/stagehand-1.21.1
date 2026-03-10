package corablue.stagehand.client.sound;

import corablue.stagehand.block.AmbienceSpeakerBlock;
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
        this.pitch = speaker.getPitch();
        this.x = speaker.getPos().getX() + 0.5;
        this.y = speaker.getPos().getY() + 0.5;
        this.z = speaker.getPos().getZ() + 0.5;

        this.volume = Math.max(1.0f, this.initialRange / 16.0f);
    }

    public void stopInstance() {
        this.setDone();
    }

    public AmbienceSpeakerBlockEntity getSpeaker() {
        return this.speaker;
    }

    @Override
    public void tick() {
        boolean isPowered = this.speaker.getCachedState().get(AmbienceSpeakerBlock.POWERED);

        if (this.speaker.isRemoved() ||
                !this.speaker.isPlaying() ||
                isPowered ||
                !this.speaker.getCurrentSound().equals(this.getId()) ||
                this.speaker.getRange() != this.initialRange ||
                this.pitch != this.speaker.getPitch()) {

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

        double distance = Math.sqrt(player.squaredDistanceTo(this.x, this.y, this.z));
        distance -= 1;
        double fadeStart = 1.0;
        double fadeEnd = this.initialRange;

        if (distance <= fadeStart) {
            this.volume = Math.max(1.0f, this.initialRange / 16.0f);
        } else if (distance >= fadeEnd) {
            this.volume = 0.0f;
        } else {
            float distancePercent = (float) ((distance - fadeStart) / (fadeEnd - fadeStart));
            float rawVolume = 1.0f - distancePercent;
            float maxVol = Math.max(1.0f, this.initialRange / 16.0f);
            this.volume = rawVolume * maxVol;
        }
    }
}