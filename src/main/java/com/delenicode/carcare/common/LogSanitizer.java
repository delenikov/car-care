package com.delenicode.carcare.common;

import java.util.regex.Pattern;

public final class LogSanitizer {
  private static final Pattern CANCEL_TOKEN_PATH = Pattern.compile("(?i)(/cancel/|/reservations/cancel/)[^/?]+");

  private LogSanitizer() {
  }

  public static String email(String email) {
    if (email == null || email.isBlank()) {
      return "unknown";
    }
    int atIndex = email.indexOf('@');
    if (atIndex <= 0 || atIndex == email.length() - 1) {
      return "invalid-email";
    }
    String local = email.substring(0, atIndex);
    String domain = email.substring(atIndex + 1);
    return local.charAt(0) + "***@" + domain;
  }

  public static String path(String path) {
    if (path == null || path.isBlank()) {
      return "/";
    }

    path = CANCEL_TOKEN_PATH.matcher(path).replaceAll("$1{token}");

    return path
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("\t", "\\t");
  }
}
