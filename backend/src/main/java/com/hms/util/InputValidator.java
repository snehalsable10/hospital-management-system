package com.hms.util;

import lombok.experimental.UtilityClass;
import java.util.regex.Pattern;

@UtilityClass
public class InputValidator {

    // Regex patterns for validation
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^[0-9\\-\\+\\s]{10,20}$");
    
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    
    private static final Pattern NAME_PATTERN = 
        Pattern.compile("^[a-zA-Z\\s'-]{2,50}$");
    
    private static final Pattern ALPHANUMERIC_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_\\-]{1,100}$");
    
    // XSS dangerous characters
    private static final Pattern XSS_PATTERN = 
        Pattern.compile("[<>\"'%;()&+]");

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches() && email.length() <= 100;
    }

    /**
     * Validate phone number
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Validate username (alphanumeric + underscore, 3-20 chars)
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    /**
     * Validate name (letters, spaces, hyphens, apostrophes)
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return NAME_PATTERN.matcher(name.trim()).matches();
    }

    /**
     * Validate password (minimum 8 chars, must contain uppercase, lowercase, number, special char)
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        // Must have: uppercase, lowercase, digit, special character
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?/].*");
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar;
    }

    /**
     * Check if string contains potential XSS characters
     */
    public static boolean containsXSSCharacters(String input) {
        if (input == null) {
            return false;
        }
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * Sanitize string by removing/escaping dangerous characters
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        
        return input
            .trim()
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("%", "&#x25;")
            .replace(";", "&#x3B;");
    }

    /**
     * Validate string length
     */
    public static boolean isValidLength(String input, int minLength, int maxLength) {
        if (input == null) {
            return minLength == 0;
        }
        int length = input.trim().length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validate numeric value is in range
     */
    public static boolean isInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    /**
     * Validate numeric value is positive
     */
    public static boolean isPositive(long value) {
        return value > 0;
    }

    /**
     * Validate numeric value is non-negative
     */
    public static boolean isNonNegative(long value) {
        return value >= 0;
    }

    /**
     * Validate decimal value is positive
     */
    public static boolean isPositiveDecimal(double value) {
        return value > 0 && !Double.isNaN(value) && !Double.isInfinite(value);
    }

    /**
     * Validate alphanumeric string (no special chars except dash, underscore)
     */
    public static boolean isValidAlphanumeric(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        return ALPHANUMERIC_PATTERN.matcher(input.trim()).matches();
    }

    /**
     * Remove null byte characters (null injection prevention)
     */
    public static String removeNullBytes(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\0", "");
    }

    /**
     * Validate string does not contain SQL injection keywords
     */
    public static boolean containsSQLInjectionPattern(String input) {
        if (input == null) {
            return false;
        }
        
        String upperInput = input.toUpperCase();
        String[] sqlKeywords = {"DROP", "DELETE", "INSERT", "UPDATE", "UNION", "SELECT", "EXEC", "EXECUTE"};
        
        for (String keyword : sqlKeywords) {
            if (upperInput.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}