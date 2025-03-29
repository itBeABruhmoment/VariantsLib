package variants_lib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.combat.CombatFleetManager;
import com.fs.starfarer.combat.ai.admiral.AdmiralAI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;

public class vlStartAdmiralAI implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String s, @NotNull BaseCommand.CommandContext commandContext) {
        CombatFleetManager manager = (CombatFleetManager) Global.getCombatEngine().getFleetManager(FleetSide.PLAYER);
        manager.setAdmiralAI(new AdmiralAI(manager, false));
        return CommandResult.SUCCESS;
    }
}
