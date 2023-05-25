package variants_lib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;

public class TestFleetInteraction extends FleetInteractionDialogPluginImpl {
    CampaignFleetAPI ally;
    ArrayList<FleetMemberAPI> restore;

    TestFleetInteraction(CampaignFleetAPI ally, CampaignFleetAPI enemy, ArrayList<FleetMemberAPI> restore) {
        super();
        this.ally = ally;
        this.otherFleet = enemy;
        this.restore = restore;
    }

    // decompiled code
    @Override
    protected void pullInNearbyFleets() {
        BattleAPI b = this.context.getBattle();
        if (!this.ongoingBattle)
            b.join(Global.getSector().getPlayerFleet());
        BattleAPI.BattleSide playerSide = b.pickSide(Global.getSector().getPlayerFleet());
        boolean hostile = (this.otherFleet.getAI() != null && this.otherFleet.getAI().isHostileTo(this.playerFleet));
        if (this.ongoingBattle)
            hostile = true;
        CampaignFleetAPI actualPlayer = Global.getSector().getPlayerFleet();
        CampaignFleetAPI actualOther = (CampaignFleetAPI)this.dialog.getInteractionTarget();
        this.pulledIn.clear();


        this.pulledIn.add(ally);


        if (this.config.pullInStations && !b.isStationInvolved()) {
            SectorEntityToken closestEntity = null;
            CampaignFleetAPI closest = null;
            Pair<SectorEntityToken, CampaignFleetAPI> p = Misc.getNearestStationInSupportRange(actualOther);
            if (p != null) {
                closestEntity = (SectorEntityToken)p.one;
                closest = (CampaignFleetAPI)p.two;
            }
            if (closest != null) {
                BattleAPI.BattleSide joiningSide = b.pickSide(closest, true);
                boolean canJoin = (joiningSide != BattleAPI.BattleSide.NO_JOIN);
                if (!this.config.pullInAllies && joiningSide == playerSide)
                    canJoin = false;
                if (!this.config.pullInEnemies && joiningSide != playerSide)
                    canJoin = false;
                if (b == closest.getBattle())
                    canJoin = false;
                if (closest.getBattle() != null)
                    canJoin = false;
                if (canJoin) {
                    if (closestEntity != null)
                        closestEntity.getMarket().reapplyIndustries();
                    b.join(closest);
                    this.pulledIn.add(closest);
                    if (!this.config.straightToEngage && this.config.showPullInText) {
                        if (b.getSide(playerSide) == b.getSideFor(closest)) {
                            this.textPanel.addParagraph(String.valueOf(Misc.ucFirst(closest.getNameWithFactionKeepCase())) + ": supporting your forces.");
                        } else if (hostile) {
                            this.textPanel.addParagraph(String.valueOf(Misc.ucFirst(closest.getNameWithFactionKeepCase())) + ": supporting the enemy.");
                        } else {
                            this.textPanel.addParagraph(String.valueOf(Misc.ucFirst(closest.getNameWithFactionKeepCase())) + ": supporting the opposing side.");
                        }
                        this.textPanel.highlightFirstInLastPara(String.valueOf(closest.getNameWithFactionKeepCase()) + ":", closest.getFaction().getBaseUIColor());
                    }
                }
            }
        }
        for (CampaignFleetAPI fleet : actualPlayer.getContainingLocation().getFleets()) {
            if (b == fleet.getBattle() || fleet.getBattle() != null)
                continue;
            if (fleet.isStationMode())
                continue;
            float dist = Misc.getDistance(actualOther.getLocation(), fleet.getLocation());
            dist -= actualOther.getRadius();
            dist -= fleet.getRadius();
            if (fleet.getFleetData().getNumMembers() <= 0)
                continue;
            float baseSensorRange = this.playerFleet.getBaseSensorRangeToDetect(fleet.getSensorProfile());
            boolean visible = fleet.isVisibleToPlayerFleet();
            SectorEntityToken.VisibilityLevel level = fleet.getVisibilityLevelToPlayerFleet();
            float joinRange = Misc.getBattleJoinRange();
            if (fleet.getFaction().isPlayerFaction() && !fleet.isStationMode())
                joinRange += Global.getSettings().getFloat("battleJoinRangePlayerFactionBonus");
            if (dist < joinRange && (dist < baseSensorRange || (visible && level != SectorEntityToken.VisibilityLevel.SENSOR_CONTACT)) && ((fleet.getAI() != null && fleet.getAI().wantsToJoin(b, true)) || fleet.isStationMode())) {
                boolean ignore = (fleet.getMemoryWithoutUpdate() != null && fleet.getMemoryWithoutUpdate().getBoolean("$cfai_ignoreOtherFleets"));
                if (ignore)
                    continue;
                BattleAPI.BattleSide joiningSide = b.pickSide(fleet, true);
                if ((!this.config.pullInAllies && joiningSide == playerSide) || (!this.config.pullInEnemies && joiningSide != playerSide))
                    continue;
                b.join(fleet);
                this.pulledIn.add(fleet);
                if (!this.config.straightToEngage && this.config.showPullInText) {
                    if (b.getSide(playerSide) == b.getSideFor(fleet)) {
                        this.textPanel.addParagraph(String.valueOf(Misc.ucFirst(fleet.getNameWithFactionKeepCase())) + ": supporting your forces.");
                    } else if (hostile) {
                        this.textPanel.addParagraph(String.valueOf(Misc.ucFirst(fleet.getNameWithFactionKeepCase())) + ": joining the enemy.");
                    } else {
                        this.textPanel.addParagraph(String.valueOf(Misc.ucFirst(fleet.getNameWithFactionKeepCase())) + ": supporting the opposing side.");
                    }
                    this.textPanel.highlightFirstInLastPara(String.valueOf(fleet.getNameWithFactionKeepCase()) + ":", fleet.getFaction().getBaseUIColor());
                }
            }
        }
        if (this.otherFleet != null)
            this.otherFleet.inflateIfNeeded();
        for (CampaignFleetAPI curr : this.pulledIn)
            curr.inflateIfNeeded();
        if (!this.ongoingBattle) {
            b.genCombined();
            b.takeSnapshots();
            this.playerFleet = b.getPlayerCombined();
            this.otherFleet = b.getNonPlayerCombined();
            if (!this.config.straightToEngage)
                showFleetInfo();
        }
    }

    protected void winningPath() {
        this.options.clearOptions();
        FleetEncounterContextPlugin.DataForEncounterSide playerData = this.context.getDataFor(this.playerFleet);
        this.context.getDataFor(this.otherFleet).setDisengaged(true);
//        if (!this.firedVictoryTriggers) {
//            SectorEntityToken prev = this.dialog.getInteractionTarget();
//            RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl();
//            plugin.setEmbeddedMode(true);
//            this.dialog.setPlugin(plugin);
//            BattleAPI battleAPI = this.context.getBattle();
//            boolean firedAnyTriggers = false;
//            for (CampaignFleetAPI other : battleAPI.getNonPlayerSide()) {
//                this.dialog.setInteractionTarget((SectorEntityToken)other);
//                plugin.init(this.dialog);
//                MemoryAPI mem = other.getMemoryWithoutUpdate();
//                List<FleetMemberAPI> losses = Misc.getSnapshotMembersLost(other);
//                List<FleetMemberAPI> remaining = other.getFleetData().getMembersListCopy();
//                int fpTotal = 0;
//                int fpLost = 0;
//                int fpRemaining = 0;
//                for (FleetMemberAPI curr : losses) {
//                    fpLost += curr.getFleetPointCost();
//                    fpTotal += curr.getFleetPointCost();
//                }
//                for (FleetMemberAPI curr : remaining) {
//                    fpRemaining += curr.getFleetPointCost();
//                    fpTotal += curr.getFleetPointCost();
//                }
//                mem.set("$someShipsDestroyed", Boolean.valueOf(!losses.isEmpty()), 0.0F);
//                mem.set("$fpLost", Integer.valueOf(fpLost), 0.0F);
//                mem.set("$fpRemaining", Integer.valueOf(fpRemaining), 0.0F);
//                mem.set("$fpLostFraction", Float.valueOf(fpLost / Math.max(1, fpTotal)), 0.0F);
//                mem.set("$battle", battleAPI, 0.0F);
//                List<String> triggers = Misc.getDefeatTriggers(other, false);
//                if (triggers != null)
//                    for (String trigger : new ArrayList<String>(triggers)) {
//                        boolean fired = FireBest.fire(null, this.dialog, plugin.getMemoryMap(), trigger);
//                        if (fired) {
//                            triggers.remove(trigger);
//                            firedAnyTriggers = true;
//                        }
//                    }
//                Misc.clearDefeatTriggersIfNeeded(other);
//            }
//            this.dialog.setInteractionTarget(prev);
//            this.dialog.setPlugin(this);
//            this.firedVictoryTriggers = true;
//            if (firedAnyTriggers) {
//                this.options.addOption("Continue", OptionId.CONTINUE_FROM_VICTORY_TRIGGERS, null);
//                return;
//            }
//        }
        CampaignFleetAPI actualPlayer = Global.getSector().getPlayerFleet();

        boolean validFleet = this.playerFleet.isValidPlayerFleet();
        BattleAPI battle = this.context.getBattle();

        if (!this.context.getLoot().isEmpty() && validFleet) {
            this.options.addOption("Pick through the wreckage", OptionId.CONTINUE_LOOT, null);
        } else {
            if (!validFleet)
                addText(getString("finalOutcomeNoShipsLeft"));
            String leave = "Leave";
            boolean withEscape = true;
            if (this.config.noSalvageLeaveOptionText != null && validFleet && this.context.getLoot().isEmpty()) {
                leave = this.config.noSalvageLeaveOptionText;
                withEscape = false;
            }
            this.options.addOption(leave, OptionId.LEAVE, null);
            if (withEscape)
                this.options.setShortcut(OptionId.LEAVE, 1, false, false, false, true);
        }

        cleanUp();
    }

    private void cleanUp() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        for(FleetMemberAPI memberAPI : player.getFleetData().getMembersListCopy()) {
            player.getFleetData().removeFleetMember(memberAPI);
        }
        for(FleetMemberAPI memberAPI : restore) {
            if(!memberAPI.isFighterWing()) {
                player.getFleetData().addFleetMember(memberAPI);
            }
        }
        playerFleet.setAIMode(false);
    }
}
