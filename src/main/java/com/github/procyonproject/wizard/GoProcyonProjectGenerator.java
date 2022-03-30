package com.github.procyonproject.wizard;

import com.goide.appengine.GoAppEngineIcons;
import com.goide.wizard.GoProjectGenerator;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.ProjectGeneratorPeer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GoProcyonProjectGenerator extends GoProjectGenerator<GoNewProcyonApplicationProjectSettings> {

    @Override
    protected void doGenerateProject(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull GoNewProcyonApplicationProjectSettings goNewProcyonApplicationProjectSettings, @NotNull Module module) {

    }

    @Override
    public @NotNull ProjectGeneratorPeer<GoNewProcyonApplicationProjectSettings> createPeer() {
        return new GoProcyonApplicationProjectGeneratorPeer();
    }

    @Override
    public @Nullable String getDescription() {
        return "Procyon framework support";
    }

    @Override
    public @NotNull @NlsContexts.Label String getName() {
        return "Procyon Initializer";
    }

    @Override
    public @Nullable Icon getLogo() {
        return GoAppEngineIcons.ICON;
    }

    @Override
    public @NotNull ValidationResult validate(@NotNull String baseDirPath) {
        return null;
    }
}
