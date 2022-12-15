package variants_lib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import variants_lib.data.OfficerFactory;

import java.util.ArrayList;

/**
 * Test command that you probably shouldn't touch
 */
public class vlTestCommand implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String s, @NotNull CommandContext commandContext) {
        int cmdNumber;
        try {
            cmdNumber = Integer.parseInt(s);
        } catch (Exception e) {
            Console.showMessage(e.toString());
            return CommandResult.ERROR;
        }

        if(cmdNumber == 0) {
            testOfficerFactory();
        }
        return CommandResult.SUCCESS;
    }

    public void testOfficerFactory() {
        final OfficerFactory fact = new OfficerFactory(Global.getSector().getFaction("pirates"));
        fact.percentEliteSkills = 0.25f;
        fact.level = 5;
        fact.skillsToAdd.add(Skills.POLARIZED_ARMOR);
        fact.skillsToAdd.add(Skills.BEST_OF_THE_BEST);
        final PersonAPI person = fact.makeOfficer();
        for(MutableCharacterStatsAPI.SkillLevelAPI skill : person.getStats().getSkillsCopy()) {
            Console.showMessage(skill.getSkill().getId());
        }
        Console.showMessage(person.getPersonalityAPI().getId());
    }
}
