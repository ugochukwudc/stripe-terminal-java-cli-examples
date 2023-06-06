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

  /**
   * Parses byte addresses from the string ip address to a byte array.
   * @param ipAddress - ipAddress as a string; ex: "192.168.0.2"
   * @return a `byte[]` of the IP addresses.
   */
  public static byte[] parseIpAddress(@NotNull String ipAddress) {
    String[] addr = ipAddress.split("\\.");
    if (addr.length != 4)
      throw new IllegalArgumentException("ip should be a x.x.x.x");
    byte[] bytes = new byte[4];
    for (int i = 0; i < 4; i++) {
      int a = Integer.parseInt(addr[i]);
      if (a < 0 || a > 255) throw new IllegalArgumentException("Invalid ip address, must be between 0 & 255");
      bytes[i] = (byte) (a & 0xFF);
    }

    return bytes;
  }
}
