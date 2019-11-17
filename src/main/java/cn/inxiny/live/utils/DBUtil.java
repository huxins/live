package cn.inxiny.live.utils;

import org.springframework.beans.factory.annotation.Value;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {

    @Value("${db.driver}")
    private static String driver;//驱动
    @Value("${db.url}")
    private static String url; //JDBC连接URL
    @Value("${db.user}")
    private static String user; //用户名
    @Value("${db.password}")
    private static String password; //密码

    static {
        try {
            //加载驱动
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("postgresql驱动加载出错！");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        Connection con = null;
        try {
            con = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
        };

        return con;
    }
}
