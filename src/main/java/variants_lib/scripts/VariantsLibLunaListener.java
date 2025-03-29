package variants_lib.scripts;

import com.fs.starfarer.api.Global;
import lunalib.lunaSettings.LunaSettingsListener;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import variants_lib.data.CommonStrings;
import variants_lib.data.SettingsData;

// ideally I would implement the mod plugin as a LunaSettingsListener but this needs to be a separate class
// for when Luna Lib isn't enabled
public class VariantsLibLunaListener implements LunaSettingsListener {
    private static final Logger log = Global.getLogger(variants_lib.scripts.VariantsLibModPlugin.class);
    static {
        log.setLevel(Level.ALL);
    }

    private VariantsLibModPlugin variantsLibModPlugin = null;

    private VariantsLibLunaListener() {}

    VariantsLibLunaListener(@NotNull VariantsLibModPlugin variantsLibModPlugin) {
        this.variantsLibModPlugin = variantsLibModPlugin;
    }

    @Override
    public void settingsChanged(@NotNull String modId) {
        boolean reloadData = false;
        for(VariantsLibPostApplicationLoadScript script : SettingsData.getInstance().getPostVariantsLibApplicationLoadScript().values()) {
            if(script.getOriginMod().equals(modId) && script.reloadWhenLunaSettingsForOriginModChanged()) {
                reloadData = true;
            }
        }
        if(reloadData) {
            log.info(CommonStrings.MOD_ID + ": settings changed, reloading data");
            try {
                variantsLibModPlugin.clearAndLoadData();
            } catch (Exception e) {
                log.error("exception during loading variants lib related data");
                log.error(e);
            }
        }
    }
}
