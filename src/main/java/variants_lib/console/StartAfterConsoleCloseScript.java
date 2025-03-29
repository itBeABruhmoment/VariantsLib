package variants_lib.console;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.BattleCreationContext;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.lazywizard.console.Console;

public class StartAfterConsoleCloseScript implements EveryFrameScript {
    private static final Logger log = Global.getLogger(StartAfterConsoleCloseScript.class);
    static {
        log.setLevel(Level.ALL);
    }

    private boolean isDone = false;
    public Runnable run = null;

    public StartAfterConsoleCloseScript(Runnable run) {
        this.run = run;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        final CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if(!isDone && !ui.isShowingDialog() && !ui.isShowingMenu()) {
            isDone = true;
            run.run();
        }
    }
}