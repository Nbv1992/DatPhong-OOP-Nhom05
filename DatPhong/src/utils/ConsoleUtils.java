package utils;

/**
 * Tiện ích in ấn ra console cho giao diện dòng lệnh.
 */
public class ConsoleUtils {

    public static final String SEPARATOR = "=".repeat(60);
    public static final String THIN_SEP  = "-".repeat(60);

    public static void printSeparator() {
        System.out.println(SEPARATOR);
    }

    public static void printThinSeparator() {
        System.out.println(THIN_SEP);
    }

    public static void printHeader(String title) {
        printSeparator();
        System.out.printf("  %s%n", title);
        printSeparator();
    }

    public static void printSuccess(String message) {
        System.out.println("[✔] " + message);
    }

    public static void printError(String message) {
        System.out.println("[✘] LỖI: " + message);
    }

    public static void printInfo(String message) {
        System.out.println("[i] " + message);
    }

    public static void printWarning(String message) {
        System.out.println("[!] CẢNH BÁO: " + message);
    }

    /**
     * Format tiền tệ VNĐ.
     */
    public static String formatCurrency(double amount) {
        if (amount == 0) return "Miễn phí";
        return String.format("%,.0fđ", amount);
    }
}
