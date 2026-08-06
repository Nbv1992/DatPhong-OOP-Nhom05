package repository;

import model.Account;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Đọc/ghi tài khoản từ file accounts.txt
 * Định dạng mỗi dòng: studentId,passwordHash,email
 */
public class AccountRepository {

    private final String filePath;
    private final Map<String, Account> cache = new LinkedHashMap<>();

    public AccountRepository(String filePath) {
        this.filePath = filePath;
        ensureFileExists();
        loadFromFile();
    }

    private void ensureFileExists() {
        try {
            Path p = Paths.get(filePath);
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            if (!Files.exists(p)) Files.createFile(p);
        } catch (IOException e) {
            System.err.println("[AccountRepository] Không thể tạo file: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        cache.clear();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                Account acc = Account.fromCsvString(line);
                if (acc != null) cache.put(acc.getStudentId().toLowerCase(), acc);
            }
        } catch (IOException e) {
            System.err.println("[AccountRepository] Lỗi đọc file: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(filePath, false), StandardCharsets.UTF_8))) {
            for (Account acc : cache.values()) pw.println(acc.toCsvString());
        } catch (IOException e) {
            System.err.println("[AccountRepository] Lỗi ghi file: " + e.getMessage());
        }
    }

    public Optional<Account> findByStudentId(String studentId) {
        return Optional.ofNullable(cache.get(studentId.toLowerCase()));
    }

    public boolean existsByStudentId(String studentId) {
        return cache.containsKey(studentId.toLowerCase());
    }

    public void save(Account account) {
        cache.put(account.getStudentId().toLowerCase(), account);
        saveToFile();
    }

    public List<Account> findAll() {
        return new ArrayList<>(cache.values());
    }
}
