package com.github.procyonprojects.util;

import com.goide.execution.GoBuildingRunConfiguration;
import com.goide.execution.application.GoApplicationConfiguration;
import com.goide.execution.application.GoApplicationRunConfigurationType;
import com.goide.sdk.GoSdk;
import com.goide.sdk.GoSdkUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

public class RunConfigurationUtils {

    public static void createRunConfigurations(@NotNull Project project, @NotNull GoSdk sdk, @NotNull VirtualFile mainFile) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ReadAction.run(() -> {
                if (!project.isDisposed()) {
                    RunManager runManager = RunManager.getInstance(project);
                    RunnerAndConfigurationSettings configurationSettings = createBuildRunConfiguration(project, runManager, mainFile);
                    if (configurationSettings != null) {
                        runManager.addConfiguration(configurationSettings);
                        runManager.setSelectedConfiguration(configurationSettings);
                    }
                }
            });
        });
    }

    private static RunnerAndConfigurationSettings createBuildRunConfiguration(@NotNull Project project, @NotNull RunManager runManager, @NotNull VirtualFile mainFile) {
        if (!mainFile.isValid()) {
            return null;
        }

        GoApplicationRunConfigurationType configurationType = GoApplicationRunConfigurationType.getInstance();
        ConfigurationFactory configurationFactory = configurationType.getConfigurationFactories()[0];
        RunnerAndConfigurationSettings configurationSettings = runManager.createConfiguration("Procyon Application", configurationFactory);
        RunConfiguration configuration = configurationSettings.getConfiguration();
        if (configuration instanceof GoApplicationConfiguration) {
            VirtualFile directory = mainFile.getParent();
            String importPath = GoSdkUtil.getImportPath(PsiManager.getInstance(project).findDirectory(directory), false);
            if (StringUtils.isNotEmpty(importPath)) {
                ((GoApplicationConfiguration)configuration).setPackage(importPath);
                ((GoApplicationConfiguration)configuration).setKind(GoBuildingRunConfiguration.Kind.PACKAGE);
            } else {
                ((GoApplicationConfiguration)configuration).setPackage(directory.getPresentableUrl());
                ((GoApplicationConfiguration)configuration).setKind(GoBuildingRunConfiguration.Kind.DIRECTORY);
            }

            ((GoApplicationConfiguration)configuration).setWorkingDirectory(directory.getPresentableUrl());
        }
        //configuration.setBeforeRunTasks(List.of());
        return configurationSettings;
    }
}
