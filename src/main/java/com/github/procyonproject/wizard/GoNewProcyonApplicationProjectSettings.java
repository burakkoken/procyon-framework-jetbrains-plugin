package com.github.procyonproject.wizard;

import com.goide.sdk.GoSdk;
import com.goide.wizard.GoNewGopathBasedProjectSettings;
import org.jetbrains.annotations.NotNull;

public class GoNewProcyonApplicationProjectSettings extends GoNewGopathBasedProjectSettings {

    public GoNewProcyonApplicationProjectSettings(@NotNull GoSdk sdk, boolean indexEntireGoPath) {
        super(sdk, indexEntireGoPath);
    }

}
