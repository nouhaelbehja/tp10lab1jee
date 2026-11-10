package com.example.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.logging.Logger;

/** Execute uniquement les scripts SQL simples livres avec le projet (sans procedures). */
public class MigrationExecutor {
    private static final Logger LOG = Logger.getLogger(MigrationExecutor.class.getName());
    private final String url, user, password;
    public MigrationExecutor(String url, String user, String password) {
        this.url = url; this.user = user; this.password = password;
    }
    public void migrate() throws Exception {
        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS schema_history (version INTEGER PRIMARY KEY, script VARCHAR(200) NOT NULL)");
            apply(c, 1, "db/migration/V1__initial.sql");
            apply(c, 2, "db/migration/V2__description_indexes.sql");
        }
    }
    private void apply(Connection c, int version, String path) throws Exception {
        try (PreparedStatement p = c.prepareStatement("SELECT version FROM schema_history WHERE version = ?")) {
            p.setInt(1, version);
            try (ResultSet r = p.executeQuery()) { if (r.next()) return; }
        }
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new FileNotFoundException(path);
        String sql;
        try (stream) { sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8); }
        sql = sql.replaceAll("(?m)--[^\\r\\n]*", "");
        try (Statement s = c.createStatement()) {
            for (String command : sql.split(";")) {
                if (!command.isBlank()) s.execute(command.trim());
            }
        }
        try (PreparedStatement p = c.prepareStatement("INSERT INTO schema_history(version, script) VALUES (?, ?)")) {
            p.setInt(1, version); p.setString(2, path); p.executeUpdate();
        }
        LOG.info("Migration appliquee : " + path);
    }
}
