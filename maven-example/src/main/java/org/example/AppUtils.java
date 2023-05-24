package org.example;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class AppUtils {
  public static File appDataDirectory(@NotNull String appName) {
    String os = System.getProperty("os.name");
    Path path;
    if (os.toUpperCase().contains("WIN")) {
      path = FileSystems.getDefault().getPath(System.getenv("AppData"), appName);
    } else {
      // For Mac and Linux
      // Get the {User Home}/Library/Application Support
      path = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Library", "Application Support", appName);
    }
    File file = path.toFile();
    if (!file.exists()) {
      file.mkdirs();
    }
    return file;
  }
}
