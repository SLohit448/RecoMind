package com.recomind.service;

import com.google.gson.Gson;
import com.recomind.dao.PreferenceDao;
import com.recomind.dao.PreferenceDaoImpl;
import com.recomind.dao.UserDao;
import com.recomind.dao.UserDaoImpl;
import com.recomind.exception.AuthenticationException;
import com.recomind.exception.DAOException;
import com.recomind.exception.ValidationException;
import com.recomind.model.User;
import com.recomind.model.UserPreference;
import com.recomind.util.DBUtil;
import com.recomind.util.SessionKeys;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

/** Login, registration and password change. Passwords are stored only as BCrypt hashes. */
public class AuthService {
    /** Result of a successful login or registration. */
    public record AuthResult(long userId, String email, String role) { }

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72; // BCrypt limit
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_ONBOARDING_CATEGORIES = 10;
    private static final int BCRYPT_ROUNDS = 10;
    private static final String INVALID_LOGIN = "Invalid email or password";

    private final UserDao userDao = new UserDaoImpl();
    private final PreferenceDao preferenceDao = new PreferenceDaoImpl();

    public AuthResult login(String email, String password) {
        String normalized = normalize(email);
        User user = userDao.findByEmail(normalized);
        boolean ok = false;
        if (user != null && user.isActive() && password != null) {
            try {
                ok = BCrypt.checkpw(password, user.getPasswordHash());
            } catch (IllegalArgumentException e) {
                ok = false;
            }
        }
        if (!ok) {
            throw new AuthenticationException(INVALID_LOGIN);
        }
        return new AuthResult(user.getId(), user.getEmail(), roleName(user.getRoleId()));
    }

    /** Creates the user and the initial preferences (onboarding categories) in one transaction. */
    public AuthResult register(String email, String password, List<Long> categories) {
        String normalized = normalize(email);
        if (!isValidEmail(normalized)) {
            throw new ValidationException("Please enter a valid email address");
        }
        validatePassword(password);
        if (userDao.findByEmail(normalized) != null) {
            throw new ValidationException("This email is already registered");
        }
        List<Long> cats = categories == null ? List.of()
                : categories.stream().filter(c -> c != null && c > 0).distinct()
                        .limit(MAX_ONBOARDING_CATEGORIES).toList();
        String prefsJson = new Gson().toJson(Map.of("categories", cats));

        User user = new User();
        user.setEmail(normalized);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS)));
        user.setActive(true);
        user.setRoleId(roleId(SessionKeys.ROLE_USER));

        long[] newId = new long[1];
        TransactionManager.runInTransaction(() -> {
            userDao.create(user);
            User saved = userDao.findByEmail(normalized);
            newId[0] = saved.getId();
            preferenceDao.create(new UserPreference(saved.getId(), prefsJson));
        });
        return new AuthResult(newId[0], normalized, SessionKeys.ROLE_USER);
    }

    public void changePassword(long userId, String oldPassword, String newPassword) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new AuthenticationException("Not logged in");
        }
        boolean ok = false;
        try {
            ok = oldPassword != null && BCrypt.checkpw(oldPassword, user.getPasswordHash());
        } catch (IllegalArgumentException e) {
            ok = false;
        }
        if (!ok) {
            throw new ValidationException("Current password is incorrect");
        }
        validatePassword(newPassword);
        user.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt(BCRYPT_ROUNDS)));
        userDao.update(user);
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= MAX_EMAIL_LENGTH && EMAIL.matcher(email).matches();
    }

    public static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Password is too long");
        }
        boolean letter = password.chars().anyMatch(Character::isLetter);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!letter || !digit) {
            throw new ValidationException("Password must contain at least one letter and one digit");
        }
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String roleName(long roleId) {
        String sql = "SELECT NAME FROM ROLES WHERE ID = ?";
        try (Connection conn = DBUtil.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : SessionKeys.ROLE_USER;
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    private long roleId(String name) {
        String sql = "SELECT ID FROM ROLES WHERE NAME = ?";
        try (Connection conn = DBUtil.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new DAOException("Role not found: " + name);
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }
}