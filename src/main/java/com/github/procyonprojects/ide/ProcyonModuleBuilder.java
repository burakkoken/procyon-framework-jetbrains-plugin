package com.github.procyonprojects.ide;

import com.github.procyonprojects.Constants;
import com.github.procyonprojects.util.RunConfigurationUtils;
import com.goide.GoIcons;
import com.goide.codeInsight.imports.GoGetPackageFix;
import com.goide.i18n.GoBundle;
import com.goide.project.GoModuleBuilderBase;
import com.goide.project.GoProjectLibrariesService;
import com.goide.psi.GoFile;
import com.goide.psi.impl.GoPackage;
import com.goide.psi.impl.GoPsiImplUtil;
import com.goide.sdk.GoSdk;
import com.goide.sdk.GoSdkService;
import com.goide.sdk.GoSdkUtil;
import com.goide.sdk.GoSdkVersion;
import com.goide.util.GoExecutor;
import com.goide.util.GoImportsSorter;
import com.goide.vendor.GoVendoringUtil;
import com.goide.vgo.configuration.VgoProjectSettings;
import com.goide.vgo.configuration.VgoSettings;
import com.goide.vgo.mod.quickfix.VgoSyncDependencyFix;
import com.goide.vgo.wizard.VgoNewProjectSettings;
import com.goide.wizard.GoProjectGeneratorPeer;
import com.intellij.DynamicBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class ProcyonModuleBuilder extends GoModuleBuilderBase<VgoNewProjectSettings> {

    protected ProcyonModuleBuilder(@NotNull GoProjectGeneratorPeer<VgoNewProjectSettings> peer) {
        super(new ProcyonProjectGeneratorPeer());
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getPresentableName() {
        return Constants.PLUGIN_NAME;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription() {
        return Constants.PLUGIN_DESCRIPTION;
    }

    @Override
    public Icon getNodeIcon() {
        return GoIcons.ICON;
    }

    @Override
    protected void moduleCreated(@NotNull Module module, boolean isCreatingNewProject) {
        createProcyonApplication(module, this.getSettings(), isCreatingNewProject, this.getContentEntryPath(), this);
    }

    public static void createProcyonApplication(@NotNull Module module,
                                                @NotNull VgoNewProjectSettings settings,
                                                boolean isCreatingNewProject,
                                                @Nullable String contentRoot,
                                                @NotNull Object requestor) {
        if (isCreatingNewProject) {
            GoProjectLibrariesService.getInstance(module.getProject()).setIndexEntireGopath(false);
        }

        Project project = module.getProject();
        GoSdk sdk = GoSdkService.getInstance(project).getSdk(null);
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            createProjectFiles(project, module, sdk, contentRoot, requestor);
        }));

        if (isCreatingNewProject || !VgoProjectSettings.getInstance(module.getProject()).isIntegrationEnabled()) {
            VgoProjectSettings vgoProjectSettings = VgoProjectSettings.getInstance(module.getProject());
            vgoProjectSettings.setIntegrationEnabled(true);
            vgoProjectSettings.setAutoVendoringMode(settings.vendoringMode);
            vgoProjectSettings.setEnvironment(settings.environment);
            VgoSettings.getInstance().addEnvironmentVars(settings.environment);
        }
    }

    private static void createProjectFiles(@NotNull Project project,
                                          @NotNull Module module,
                                          @NotNull GoSdk sdk,
                                          @Nullable String contentRoot,
                                          @NotNull Object requestor) {
        if (StringUtils.isEmpty(contentRoot)) {
            return;
        }

        try {
            VirtualFile directory = VfsUtil.createDirectoryIfMissing(contentRoot);
            if (directory != null) {
                createGoModFile(module, directory);
                VirtualFile mainFile = createMainFile(module, directory, requestor);
                createInitFile(module, directory, requestor);

                VirtualFile resourcesDir = VfsUtil.createDirectoryIfMissing(directory, "resources");
                VirtualFile applicationPropertiesFile = createApplicationPropertiesFile(module, resourcesDir, requestor);

                VirtualFile generatedDir = VfsUtil.createDirectoryIfMissing(directory, "generated");
                createGeneratedProcyonFile(module, generatedDir, requestor);

                VirtualFile appDir = VfsUtil.createDirectoryIfMissing(directory, "app");
                VirtualFile controllerDir = VfsUtil.createDirectoryIfMissing(appDir, "controller");
                createHelloControllerFile(module, controllerDir, requestor);

                VirtualFile webDir = VfsUtil.createDirectoryIfMissing(directory, "web");

                StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> {
                    if (applicationPropertiesFile.isValid()) {
                        RunConfigurationUtils.createRunConfigurations(project, sdk, mainFile);
                    }

                    if (mainFile.isValid()) {
                        FileEditorManager.getInstance(project).openFile(mainFile, true);

                        CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                            GoFile mainFileGo = (GoFile) PsiManager.getInstance(module.getProject()).findFile(mainFile);
                            if (mainFileGo != null) {
                                GoImportsSorter.forFile(mainFileGo).sortImports();
                            }
                            formatProject(project);
                        });

                        VgoSyncDependencyFix.applyFix(project, ModuleUtilCore.findModuleForFile(mainFile, project), mainFile.getParent(),
                                GoBundle.message("go.settings.modules.sync.dependencies.fix.family.name", new Object[0]));
                        GoGetPackageFix.goGetPackage(project, module, "github.com/procyon/procyon-projects/procyon-core");
                    }
                });
            }
        } catch (IOException e) {

        }
    }

    private static void formatProject(Project project) {
        FileDocumentManager.getInstance().saveAllDocuments();;
        Iterator<Module> modules = GoSdkUtil.getGoModules(project).iterator();
        while (modules.hasNext()) {
            Module currentModule = modules.next();
            VirtualFile[] roots = ModuleRootManager.getInstance(currentModule).getContentRoots();
            for (int i = 0; i < roots.length; i++) {
                VirtualFile file = roots[i];
                String presentation = "go fmt " + file.getPath();
                GoExecutor.in(project, currentModule)
                        .disablePty()
                        .withPresentableName(presentation)
                        .withWorkDirectory(file.getPath())
                        .withParameters(new String[]{"fmt", "./..."})
                        .executeWithProgress(true, true, (executionResult -> {
                            VfsUtil.markDirtyAndRefresh(true, true, true, new VirtualFile[]{file});
                        }));
            }
        }
    }

    private static VirtualFile createGoModFile(@NotNull Module module, @NotNull VirtualFile projectDirectory) throws IOException {
        PsiDirectory projectPsiDirectory = PsiManager.getInstance(module.getProject()).findDirectory(projectDirectory);
        return createGoModFile(module, projectDirectory, projectPsiDirectory);
    }

    private static VirtualFile createGoModFile(@NotNull Module module, @NotNull VirtualFile projectDirectory, @Nullable PsiDirectory projectPsiDirectory) throws IOException {
        VirtualFile goModFile = projectDirectory.createChildData(module, "go.mod");
        String moduleName = projectPsiDirectory != null ? GoPackage.in(projectPsiDirectory, "").getImportPath(GoVendoringUtil.isVendoringEnabled(module)) : null;

        StringBuilder content = new StringBuilder();

        // module name
        String name = ObjectUtils.notNull(moduleName, projectDirectory.getName());
        content.append("module ").append(name.replace(' ', '_')).append('\n');

        // go version
        GoSdk sdk = GoSdkService.getInstance(module.getProject()).getSdk(module);
        GoSdkVersion sdkVersion = sdk.getMajorVersion();
        if (sdkVersion != GoSdkVersion.UNKNOWN && sdkVersion != GoSdkVersion.GO_DEVEL) {
            content.append("\ngo ").append(sdkVersion).append('\n');
        }

        goModFile.setBinaryContent(content.toString().getBytes(StandardCharsets.UTF_8));
        return goModFile;
    }

    private static VirtualFile createMainFile(@NotNull Module module, @NotNull VirtualFile projectDirectory, @NotNull Object requestor) throws IOException {
        String applicationName = GoPsiImplUtil.sanitizePackageName(projectDirectory.getName()).toLowerCase(DynamicBundle.getLocale());
        final String content = "package main\n\nimport (\n\t\"github.com/procyon-projects/procyon\"\n\t _ \"" + module.getName().replace(' ', '_') + "/generated\" // DO NOT DELETE.\n)\n\nfunc main() {\n\tprocyon.NewProcyonApplication().Run()\n}";
        return createFile("main.go", content, projectDirectory, requestor);
    }

    private static VirtualFile createHelloControllerFile(@NotNull Module module, @NotNull VirtualFile projectDirectory, @NotNull Object requestor) throws IOException {
        final String content = "package controller\n\nimport web \"github.com/procyon-projects/procyon-web\"\n\ntype HelloController struct{\n}\n\nfunc NewHelloController() *HelloController {\n\treturn &HelloController{}\n}\n\nfunc (controller *HelloController) RegisterHandlers(registry web.HandlerRegistry) {\n\tregistry.Register(web.Get(controller.Index, web.Path(\"/\")))\n}\n\nfunc (controller *HelloController) Index(ctx *web.WebRequestContext) {\n\tctx.Ok().SetModel(\"Greetings from Procyon!\")\n}";
        return createFile("hello.go", content, projectDirectory, requestor);
    }

    private static VirtualFile createGeneratedProcyonFile(@NotNull Module module, @NotNull VirtualFile projectDirectory, @NotNull Object requestor) throws IOException {
        final String content = "// Code generated by Procyon Framework; DO NOT EDIT.\n\n package generated\n\nfunc init() {\n}";
        return createFile("procyon.go", content, projectDirectory, requestor);
    }

    private static VirtualFile createInitFile(@NotNull Module module, @NotNull VirtualFile projectDirectory, @NotNull Object requestor) throws IOException {
        final String content = "package main\n\nimport (\n\tcore \"github.com/procyon-projects/procyon-core\"\n \"" + module.getName().replace(' ', '_') + "/app/controller\"\n)\n\nfunc init() {\n\t core.Register(controller.NewHelloController)\n}";
        return createFile("init.go", content, projectDirectory, requestor);
    }

    private static VirtualFile createApplicationPropertiesFile(@NotNull Module module, @NotNull VirtualFile projectDirectory, @NotNull Object requestor) throws IOException {
        String applicationName = GoPsiImplUtil.sanitizePackageName(projectDirectory.getName()).toLowerCase(DynamicBundle.getLocale());
        String content = "procyon:\n  application:\n    name: " + applicationName + "\n\nserver:\n  port: 8080\n\nlogging:\n  level: DEBUG";
        return createFile("procyon.yaml", content, projectDirectory, requestor);
    }

    private static VirtualFile createFile(@NonNls @NotNull String name, @NonNls @NotNull String content, @NotNull VirtualFile baseDir, @NotNull Object requestor) throws IOException {
        VirtualFile existingFile = baseDir.findChild(name);

        if (existingFile == null) {
            VirtualFile file = baseDir.createChildData(requestor, name);
            VfsUtil.saveText(file, content);
            return file;
        }

        return null;
    }
}
