package variants_lib.scripts;

public interface VariantsLibPostApplicationLoadScript {
    public void runPostApplicationLoadScript();
    public String getOriginMod();
    public boolean reloadWhenLunaSettingsForOriginModChanged();
}
