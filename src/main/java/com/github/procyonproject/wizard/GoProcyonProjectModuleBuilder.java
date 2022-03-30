package com.github.procyonproject.wizard;

import com.goide.appengine.GoAppEngineIcons;
import com.goide.appengine.wizard.GoAppEngineModuleBuilder;
import com.goide.execution.GoBuildingRunConfiguration;
import com.goide.execution.application.GoApplicationConfiguration;
import com.goide.execution.application.GoApplicationRunConfigurationType;
import com.goide.i18n.GoBundle;
import com.goide.project.GoBuildTargetSettings;
import com.goide.project.GoModuleBuilderBase;
import com.goide.project.GoModuleSettings;
import com.goide.project.GoProjectLibrariesService;
import com.goide.psi.impl.GoPsiImplUtil;
import com.goide.sdk.GoSdk;
import com.goide.sdk.GoSdkService;
import com.goide.sdk.GoSdkUtil;
import com.goide.sdk.combobox.GoBasedSdkChooserCombo;
import com.intellij.DynamicBundle;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;

public class GoProcyonProjectModuleBuilder extends GoModuleBuilderBase<GoNewProcyonApplicationProjectSettings> {
    private static final Logger LOG = Logger.getInstance(GoAppEngineModuleBuilder.class);

    public GoProcyonProjectModuleBuilder() {
        super(new GoProcyonApplicationProjectGeneratorPeer() {});
    }

    public String getPresentableName() {
        return "Procyon Initializer";
        //return GoBundle.message("go.app.engine.wizard.name", new Object[0]);
    }

    public Icon getNodeIcon() {
        return GoAppEngineIcons.ICON;
    }

    @Override
    protected void moduleCreated(@NotNull Module module, boolean isCreatingNewProject) {
        if (isCreatingNewProject) {
            GoProjectLibrariesService.getInstance(module.getProject()).setIndexEntireGopath((this.getSettings()).indexEntireGoPath);
        }

        String contentEntryPath = this.getContentEntryPath();
        VirtualFile baseDir = contentEntryPath != null ? LocalFileSystem.getInstance().findFileByPath(contentEntryPath) : null;
        if (baseDir != null) {
            createAppEngineApplication(module.getProject(), module, baseDir, this);
        }
    }

    @Nls(
            capitalization = Nls.Capitalization.Sentence
    )
    public String getDescription() {
        return "Procyon framework support";
        //return GoBundle.message("go.app.engine.wizard.description", new Object[0]);
    }

    protected static GoBasedSdkChooserCombo.Validator<GoSdk> getSdkValidator() {
        return (sdk) -> ValidationResult.OK;
    }

    public static void createAppEngineApplication(@NotNull Project project, @NotNull Module module, @NotNull VirtualFile baseDir, @NotNull Object requestor) {
        try {
            GoSdk sdk = GoSdkService.getInstance(project).getSdk((Module)null);
            GoModuleSettings moduleSettings = GoModuleSettings.getInstance(module);
            GoBuildTargetSettings buildTargetSettings = moduleSettings.getBuildTargetSettings();
            buildTargetSettings.customFlags = (String[]) ArrayUtil.append(buildTargetSettings.customFlags, "appengine");
            String applicationName = GoPsiImplUtil.sanitizePackageName(baseDir.getName()).toLowerCase(DynamicBundle.getLocale());
            VirtualFile helloWorld = (VirtualFile) WriteAction.compute(() -> {
                return createApplicationFile(applicationName, baseDir, sdk, requestor);
            });
            VirtualFile appYaml = (VirtualFile)WriteAction.compute(() -> {
                return createAppYamlFile(applicationName, baseDir, sdk, requestor);
            });
            StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> {
                if (appYaml.isValid()) {
                    FileEditorManager.getInstance(project).openFile(appYaml, true);
                    createRunConfigurations(project, sdk, helloWorld);
                }

                if (helloWorld.isValid()) {
                    FileEditorManager.getInstance(project).openFile(helloWorld, true);
                }

            });
        } catch (IOException var10) {
            LOG.error("Cannot create project template", var10);
        }

    }

    private static void createRunConfigurations(@NotNull Project project, @NotNull GoSdk sdk, @NotNull VirtualFile helloWorld) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ReadAction.run(() -> {
                if (!project.isDisposed()) {
                    RunManager runManager = RunManager.getInstance(project);
                    RunnerAndConfigurationSettings configurationSettings = sdk.supportsAppEngine() ? createBuildRunConfiguration(project, runManager, helloWorld) : null;
                    if (configurationSettings != null) {
                        runManager.addConfiguration(configurationSettings);
                        runManager.setSelectedConfiguration(configurationSettings);
                    }

                }
            });
        });
    }

    private static RunnerAndConfigurationSettings createBuildRunConfiguration(@NotNull Project project, @NotNull RunManager runManager, @NotNull VirtualFile helloWorld) {
        if (!helloWorld.isValid()) {
            return null;
        } else {
            GoApplicationRunConfigurationType configurationType = GoApplicationRunConfigurationType.getInstance();
            ConfigurationFactory configurationFactory = configurationType.getConfigurationFactories()[0];
            RunnerAndConfigurationSettings configurationSettings = runManager.createConfiguration(GoBundle.message("go.app.engine.wizard.name", new Object[0]), configurationFactory);
            RunConfiguration configuration = configurationSettings.getConfiguration();
            if (configuration instanceof GoApplicationConfiguration) {
                VirtualFile directory = helloWorld.getParent();
                String importPath = GoSdkUtil.getImportPath(PsiManager.getInstance(project).findDirectory(directory), false);
                if (StringUtil.isNotEmpty(importPath)) {
                    ((GoApplicationConfiguration)configuration).setPackage(importPath);
                    ((GoApplicationConfiguration)configuration).setKind(GoBuildingRunConfiguration.Kind.PACKAGE);
                } else {
                    ((GoApplicationConfiguration)configuration).setDirectoryPath(directory.getPresentableUrl());
                    ((GoApplicationConfiguration)configuration).setKind(GoBuildingRunConfiguration.Kind.DIRECTORY);
                }

                ((GoApplicationConfiguration)configuration).setWorkingDirectory(directory.getPresentableUrl());
            }

            return configurationSettings;
        }
    }

    private static VirtualFile createApplicationFile(@NotNull String applicationName, @NotNull VirtualFile baseDir, @NotNull GoSdk sdk, @NotNull Object requestor) throws IOException {
        String content;
        if (sdk.supportsAppEngine()) {
            content = "package main\n\nimport (\n\t\"fmt\"\n\t\"log\"\n\t\"net/http\"\n\t\"os\"\n)\n\nfunc main() {\n\thttp.HandleFunc(\"/\", indexHandler)\n\tport := os.Getenv(\"PORT\")\n\tif port == \"\" {\n\t\tport = \"8080\"\n\t\tlog.Printf(\"Defaulting to port %s\", port)\n\t}\n\n\tlog.Printf(\"Listening on port %s\", port)\n\tlog.Printf(\"Open http://localhost:%s in the browser\", port)\n\tlog.Fatal(http.ListenAndServe(fmt.Sprintf(\":%s\", port), nil))\n}\n\nfunc indexHandler(w http.ResponseWriter, r *http.Request) {\n\tif r.URL.Path != \"/\" {\n\t\thttp.NotFound(w, r)\n\t\treturn\n\t}\n\t_, err := fmt.Fprint(w, \"Hello, World!\")\n\tif err != nil {\n\t\tw.WriteHeader(http.StatusInternalServerError)\n\t}\n}";
        } else {
            content = "package " + applicationName + "\n\nimport (\n\t\"fmt\"\n\t\"net/http\"\n)\n\nfunc init() {\n\thttp.HandleFunc(\"/\", handle)\n}\n\nfunc handle(w http.ResponseWriter, r *http.Request) {\n\t_, err := fmt.Fprint(w, \"<html><body>Hello, World!</body></html>\")\n\tif err != nil {\n\t\tw.WriteHeader(http.StatusInternalServerError)\n\t}\n}";
        }

        return createFile(applicationName + ".go", content, baseDir, requestor);
    }

    private static VirtualFile createAppYamlFile(@NotNull String applicationName, @NotNull VirtualFile baseDir, GoSdk sdk, @NotNull Object requestor) throws IOException {
        String runtime = "go";
        if (sdk.supportsAppEngine()) {
            String majorVersion = GoSdkUtil.getSdkMajorVersion(sdk.getVersion());
            if (majorVersion != null) {
                runtime = "go" + majorVersion.replaceAll("\\.", "");
            }
        }

        return createFile("app.yaml", "application: " + applicationName + " \nversion: 1\nruntime: " + runtime + "\napi_version: go1\n\nhandlers:\n- url: /.*\n  script: _go_app", baseDir, requestor);
    }

    @NotNull
    private static VirtualFile createFile(@NonNls @NotNull String name, @NonNls @NotNull String content, @NotNull VirtualFile baseDir, @NotNull Object requestor) throws IOException, IOException, IOException {
        VirtualFile existingFile = baseDir.findChild(name);
        if (existingFile != null) {
            if (existingFile == null) {
            }

            return existingFile;
        } else {
            VirtualFile file = baseDir.createChildData(requestor, name);
            VfsUtil.saveText(file, content);
            if (file == null) {
            }

            return file;
        }
    }
}
