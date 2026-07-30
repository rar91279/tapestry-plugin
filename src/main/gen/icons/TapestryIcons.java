package icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * NOTE THIS FILE IS AUTO-GENERATED
 * DO NOT EDIT IT BY HAND, run "Generate icon classes" configuration instead
 */
public final class TapestryIcons {
  private static @NotNull Icon load(@NotNull String path, int cacheKey, int flags) {
    return IconManager.getInstance().loadRasterizedIcon(path, TapestryIcons.class.getClassLoader(), cacheKey, flags);
  }
  /** 16x16 */ public static final @NotNull Icon Arrow_left = load("com/intellij/tapestry/core/icons/arrow_left.png", 0, 0);
  /** 16x16 */ public static final @NotNull Icon Arrow_right = load("com/intellij/tapestry/core/icons/arrow_right.png", 0, 0);
  /** 16x16 */ public static final @NotNull Icon Bullet_go = load("com/intellij/tapestry/core/icons/bullet_go.png", 0, 0);
  /** 16x16 */ public static final @NotNull Icon CompactBasePackage = load("com/intellij/tapestry/core/icons/compactBasePackage.png", 0, 0);
  public static final @NotNull Icon Component = AllIcons.Nodes.Class;
  /** 16x16 */ public static final @NotNull Icon Components = load("com/intellij/tapestry/core/icons/components.png", 0, 0);
  /** 16x16 */ public static final @NotNull Icon Folder = load("com/intellij/tapestry/core/icons/folder.png", 0, 0);
  /** 16x16 */ public static final @NotNull Icon GroupElementFiles = load("com/intellij/tapestry/core/icons/groupElementFiles.png", 0, 0);
  /** 16x16 */ public static final @NotNull Icon House = load("com/intellij/tapestry/core/icons/house.png", 0, 0);
  public static final @NotNull Icon Mixin = AllIcons.Nodes.Method;
  /** 16x16 */ public static final @NotNull Icon Mixins = load("com/intellij/tapestry/core/icons/mixins.png", 0, 0);
  public static final @NotNull Icon Page = AllIcons.Nodes.Parameter;
  /** 16x16 */ public static final @NotNull Icon Pages = load("com/intellij/tapestry/core/icons/pages.png", 0, 0);
  public static final @NotNull Icon Tapestry_logo_small = IconLoader.getIcon("/documentation/tapestry-file-type-logo.svg", TapestryIcons.class);
  public static final @NotNull Icon TapestryToolWindow = IconLoader.getIcon("/documentation/tapestry-file-type-logo.svg", TapestryIcons.class);
}
