package variants_lib.data;

import java.util.HashMap;
import java.util.HashSet;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;

// stores important strings
public class CommonStrings {
    // file paths and other important mod info
    public static final String SETTINGS_FILE_NAME = "data/variants_lib/variants_lib_settings.json";
    public static final String VARIANT_TAGS_CSV_PATH = "data/variants_lib/variant_tags.csv";
    public static final String FACTION_TAGS_CSV_PATH = "data/variants_lib/faction_tags.csv";
    public static final String FLEETS_CSV_PATH = "data/variants_lib/fleets/fleets.csv";
    public static final String FLEETS_FOLDER_PATH = "data/variants_lib/fleets/";
    public static final String MOD_ID = "variants_lib";

    // memkeys

    public static final String DO_NOT_CHANGE_PERSONALITY_KEY = "$vlNoPersonalityChange";
    public static final String MODIFIED_IN_BATTLE_KEY = "$vlModifiedInBattle";
    public static final String FLEET_VARIANT_KEY = "$vlType";
    public static final String FLEET_EDITED_MEMKEY = "$vlmodified";

    public static final HashSet<String> PERSONALITIES =  new HashSet<String>() {{
        add(Personalities.AGGRESSIVE);  add(Personalities.CAUTIOUS); add(Personalities.RECKLESS);
        add(Personalities.STEADY);      add(Personalities.TIMID);
    }};

    // fleet types of some nex fleets
    public static final String NEX_VENGANCE_FLEET = "vengeanceFleet";
    public static final String NEX_INVASION_FLEET = "exerelinInvasionFleet";
    public static final String NEX_INVASION_SUPPORT_FLEET = "exerelinInvasionSupportFleet";
    public static final String NEX_SUPPRESSION_FLEET = "nex_suppressionFleet";
    public static final String NEX_SPECIAL_FORCES_FLEET = "nex_specialForces";
    public static final String NEX_RELIEF_FLEET = "nex_reliefFleet";
    public static final String NEX_RESPONSE_FLEET =  "exerelinResponseFleet";
    public static final String NEX_VULTURE_FLEET =  "nex_vultureFleet";
    public static final String NEX_MINING_FLEET =  "exerelinMiningFleet";

    // faction_tags.csv related strings

    public static final String NO_AUTOFIT_TAG =  "no_autofit";

    // variant_tags.csv related strings

    // fleets.csv related strings
    public static final String FLEETS_CSV_FIRST_COLUMN_NAME = "fileName";

    // the skill editing tags mapped to their corresponding skill Id's
    public static final HashMap<String, String> SKILL_EDIT_TAGS = new HashMap<String, String>() {{
        put("hs", "helmsmanship");          put("ce", "combat_endurance");          put("im", "impact_mitigation");
        put("dc", "damage_control");        put("fm", "field_modulation");          put("pd", "point_defense");
        put("ta", "target_analysis");       put("bm", "ballistic_mastery");         put("se", "systems_expertise");
        put("ms", "missile_specialization");put("gi", "gunnery_implants");          put("ew", "energy_weapon_mastery");
        put("oe", "ordnance_expert");       put("pa", "polarized_armor");
    }};

    // the personality editing tags mapped to their corresponding personality Id's
    public static final HashMap<String, String> PERSONALITY_EDIT_TAGS =  new HashMap<String, String>() {{
        put("ca", Personalities.CAUTIOUS);         put("ti", Personalities.TIMID);          put("st", Personalities.STEADY);
        put("ag", Personalities.AGGRESSIVE);       put("re", Personalities.RECKLESS);
    }};

    public static final String DO_NOT_EDIT_OFFICER = "no";

    private CommonStrings() {}
}
