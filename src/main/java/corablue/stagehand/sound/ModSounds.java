package corablue.stagehand.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    // 1. Declare your custom sound events here
    public static final SoundEvent TELEPORT_CHARGE = registerSoundEvent("teleport_charge2");
    public static final SoundEvent TELEPORT_FIRE = registerSoundEvent("teleport_fire");

    public static final SoundEvent AMBIENCE_ALARM = registerSoundEvent("ambience_alarm");
    public static final SoundEvent AMBIENCE_CLOCK = registerSoundEvent("ambience_clock");
    public static final SoundEvent AMBIENCE_CREAKING_METAL = registerSoundEvent("ambience_creaking_metal");
    public static final SoundEvent AMBIENCE_CRYSTALS = registerSoundEvent("ambience_crystals");
    public static final SoundEvent AMBIENCE_DISCHORDANT_ANGELS = registerSoundEvent("ambience_dischordant_angels");
    public static final SoundEvent AMBIENCE_ELECTRIC_HUM = registerSoundEvent("ambience_electric_hum");
    public static final SoundEvent AMBIENCE_INDUSTRIAL_HUM = registerSoundEvent("ambience_industrial_hum");
    public static final SoundEvent AMBIENCE_JUNGLE = registerSoundEvent("ambience_jungle");
    public static final SoundEvent AMBIENCE_LAVA_CAVE = registerSoundEvent("ambience_lava_cave");
    public static final SoundEvent AMBIENCE_RAIN = registerSoundEvent("ambience_rain");
    public static final SoundEvent AMBIENCE_ROOM_DRONE = registerSoundEvent("ambience_room_drone");
    public static final SoundEvent AMBIENCE_SCIFI_COMPUTERS = registerSoundEvent("ambience_scifi_computers");
    public static final SoundEvent AMBIENCE_SCIFI_ENGINE = registerSoundEvent("ambience_scifi_engine");
    public static final SoundEvent AMBIENCE_SLEEPY = registerSoundEvent("ambience_sleepy");
    public static final SoundEvent AMBIENCE_STEAM_ENGINE = registerSoundEvent("ambience_steam_engine");
    public static final SoundEvent AMBIENCE_STEAM_LEAK = registerSoundEvent("ambience_steam_leak");
    public static final SoundEvent AMBIENCE_UNDERWATER = registerSoundEvent("ambience_underwater");
    public static final SoundEvent AMBIENCE_WOOD_CREAKING = registerSoundEvent("ambience_wood_creaking");
    public static final SoundEvent AMBIENCE_SPARKS = registerSoundEvent("ambience_sparks");
    public static final SoundEvent AMBIENCE_CRICKETS = registerSoundEvent("ambience_crickets");
    public static final SoundEvent AMBIENCE_RUNNING_WATER = registerSoundEvent("ambience_running_water");
    public static final SoundEvent AMBIENCE_WIND = registerSoundEvent("ambience_wind");
    public static final SoundEvent AMBIENCE_FIREPLACE = registerSoundEvent("ambience_fireplace");
    public static final SoundEvent AMBIENCE_BOILING = registerSoundEvent("ambience_boiling");
    public static final SoundEvent AMBIENCE_DRIPPING = registerSoundEvent("ambience_dripping");
    public static final SoundEvent AMBIENCE_FORCEFIELD = registerSoundEvent("ambience_forcefield");

    // Example of a second sound:
    // public static final SoundEvent FACTORY_HUM = registerSoundEvent("factory_hum");

    // 2. The helper method that handles the actual registration
    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of("stagehand", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    // 3. The initialization method to call in your main mod class
    public static void registerSounds() {
        System.out.println("Registering Custom Sounds for Stagehand...");
    }
}