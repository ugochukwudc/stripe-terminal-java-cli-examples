package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the application utility methods in {@link AppUtils}.
 */
class AppUtilsTest {

  @Test
  void testIpAddressParsing() {
      Assertions.assertArrayEquals(new byte[] {(byte) 0xFF, 0x01, 0x00, 0x03}, AppUtils.parseIpAddress("255.1.0.3"));
      Assertions.assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, AppUtils.parseIpAddress("255.255.255.255"));
      Assertions.assertArrayEquals(new byte[] {0x0A, 0x0B, 0x0C, 0x0D}, AppUtils.parseIpAddress("10.11.12.13"));
  }

  @Test
  void testIpAddressParsingInvalidIpAddress() {
      assertThrows(IllegalArgumentException.class, () -> AppUtils.parseIpAddress("256.10.10.10"));
      assertThrows(IllegalArgumentException.class, () -> AppUtils.parseIpAddress("A.B.C.D"));
      assertThrows(IllegalArgumentException.class, () -> AppUtils.parseIpAddress("-10.10.10.10"));
  }
}