package com.github.procyonprojects.ide;

import com.github.procyonprojects.Constants;
import com.goide.GoIcons;
import com.goide.vgo.wizard.VgoNewProjectSettings;
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

public class ProcyonProjectGenerator extends GoProjectGenerator<VgoNewProjectSettings> {

    @Override
    public @NotNull ProjectGeneratorPeer<VgoNewProjectSettings> createPeer() {
        return new ProcyonProjectGeneratorPeer();
    }

    @Override
    protected void doGenerateProject(@NotNull Project project,
                                     @NotNull VirtualFile virtualFile,
                                     @NotNull VgoNewProjectSettings vgoNewProjectSettings,
                                     @NotNull Module module) {
        ProcyonModuleBuilder.createProcyonApplication(module,
                vgoNewProjectSettings,
                true,
                virtualFile.getPath(),
                this);
    }

    @Override
    public @NotNull @NlsContexts.Label String getName() {
        return Constants.PLUGIN_NAME;
    }

    @Override
    public @Nullable String getDescription() {
        return Constants.PLUGIN_DESCRIPTION;
    }

    @Override
    public @Nullable Icon getLogo() {
        return GoIcons.ICON;
    }

    @Override
    public @NotNull ValidationResult validate(@NotNull String baseDirPath) {
        return ValidationResult.OK;
    }
}
