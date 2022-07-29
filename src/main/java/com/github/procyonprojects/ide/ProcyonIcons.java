package com.github.procyonprojects.ide;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ProcyonIcons {

    private static @NotNull Icon load(@NotNull String path) {
        return IconLoader.getIcon(path, ProcyonIcons.class);
    }

    public static final class Folder {
        public static final @NotNull Icon Web = AllIcons.Nodes.WebFolder;
        public static final @NotNull Icon Module = AllIcons.Nodes.Package;
        public static final @NotNull Icon Cmd = AllIcons.RunConfigurations.Compound;
        public static final @NotNull Icon Resources = load("/icons/resourcesRoot.svg");
        public static final @NotNull Icon App = load("/icons/appRoot.svg");
        public static final @NotNull Icon Generated = load("/icons/generatedRoot.svg");
        public static final @NotNull Icon Pkg = load("/icons/pkgRoot.svg");
        public static final @NotNull Icon Internal = load("/icons/internalRoot.svg");
        public static final @NotNull Icon Configs = AllIcons.Nodes.ConfigFolder;
    }
}
