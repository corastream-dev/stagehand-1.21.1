package corablue.stagehand.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;

@Modmenu(modId = "stagehand") // Automatically adds a config screen via ModMenu
@Config(name = "stagehand-config", wrapperName = "StagehandConfig")
public class StagehandConfigModel {

    public boolean OnlyAllowEmitterInStage = false;

    public boolean OnlyAllowSpeakerInStage = false;

    public boolean DisableFatigueCore = false;

    @RangeConstraint(min = 0, max = 3)
    public int MiningFatigueLevel = 2;

    @RangeConstraint(min = 0, max = 24000)
    public int StageTimeOverride = 6000;

    public boolean AllowFatigueZoneOptout = true;

    public boolean StageDeathReturnsToSpawn = false;

    public boolean StageDoDaylightCycle = false;
}