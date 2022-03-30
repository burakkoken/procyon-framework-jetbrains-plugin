package com.github.procyonproject.wizard;

import com.goide.sdk.GoSdk;
import com.goide.sdk.combobox.GoBasedSdkChooserCombo;
import com.goide.wizard.GoGopathBasedProjectGeneratorPeer;
import org.jetbrains.annotations.NotNull;

    public class GoProcyonApplicationProjectGeneratorPeer extends GoGopathBasedProjectGeneratorPeer<GoNewProcyonApplicationProjectSettings> {
    public GoProcyonApplicationProjectGeneratorPeer() {
    }

    protected GoBasedSdkChooserCombo.Validator<GoSdk> getSdkValidator() {
        return GoProcyonProjectModuleBuilder.getSdkValidator();
    }

    @NotNull
    public GoNewProcyonApplicationProjectSettings getSettings() {
        return new GoNewProcyonApplicationProjectSettings(this.getSdkFromCombo(), this.isIndexGoPath());
    }
}
