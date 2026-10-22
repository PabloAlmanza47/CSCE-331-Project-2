package backend.database;

import backend.config.DbSetup;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Creates configured PostgreSQL connections for the application backend. */
public final class DatabaseConnection {
    private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/team1db";
    private static final int LOGIN_TIMEOUT_SECONDS = 15;
    private static final int NETWORK_TIMEOUT_MILLISECONDS = 30000;

    private DatabaseConnection() {
    }

    public static Connection open() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        Connection connection = DriverManager.getConnection(DB_URL, DbSetup.user, DbSetup.pswd);
        connection.setNetworkTimeout(Runnable::run, NETWORK_TIMEOUT_MILLISECONDS);
        return connection;
    }
}
