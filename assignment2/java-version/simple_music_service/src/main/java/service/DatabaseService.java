package service;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseService {
    private static DataSource dataSource;
    static {
            HikariConfig config = new HikariConfig();
            config.setMaximumPoolSize(Integer.parseInt(System.getenv("MAXIMUM_CONN_POOL_SIZE")));
            config.setJdbcUrl("jdbc:mysql://database-album.ckrkh8l5ycja.us-west-2.rds.amazonaws.com:3306/music_service");
            config.setUsername(System.getenv("DB_USERNAME"));
            config.setPassword(System.getenv("DB_PASSWORD"));
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
