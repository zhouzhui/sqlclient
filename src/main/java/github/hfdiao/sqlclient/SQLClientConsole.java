package github.hfdiao.sqlclient;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;

/**
 * @author dhf
 */
public class SQLClientConsole {
    public static void main(String[] args) throws ClassNotFoundException,
            EOFException, UnsupportedEncodingException, IOException,
            SQLException {
        showHelp();
        String[] splits = Readline.readline("connect>").split(" ");
        if (splits.length != 6) {
            showHelp();
            return;
        }

        int i = -1;
        String dbtype = splits[++i];
        String host = splits[++i];
        int port = Integer.parseInt(splits[++i]);
        String database = splits[++i];
        String username = splits[++i];
        String password = splits[++i];

        SQLClient client = makeClient(dbtype);
        Connection conn = client.getConnection(host, port, database, username,
                password);

        int maxHistory = 1000;
        CmdCompleter completer = new CmdCompleter(maxHistory);
        Readline.setCompleter(completer);
        try {
            while (true) {
                String line = Readline.readline("sql>");
                if ("quit".equalsIgnoreCase(line)
                        || "bye".equalsIgnoreCase(line)
                        || "exit".equalsIgnoreCase(line)) {
                    exit();
                    break;
                }
                try {
                    completer.addHistory(line);
                    Object result = client.doSql(conn, line);
                    if (result instanceof Integer) {
                        System.out.println("affected lines: " + result);
                    } else if (result instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) result;
                        for (Map<String, Object> map: list) {
                            System.out.println(map);
                        }
                    } else {
                        System.out.println(result);
                    }
                } catch (Exception e) {
                    System.out
                            .println("execute sql fail, please check your sql statement: "
                                    + line);
                    e.printStackTrace();
                }
            }
        } finally {
            closeConnection(conn);
        }
    }

    static {
        if (!loadReadline()) {
            System.out
                    .println("can't find libreadline, use java default sysin");
            Readline.load(ReadlineLibrary.PureJava);
        }
    }

    private static boolean loadReadline() {
        ReadlineLibrary[] libs = new ReadlineLibrary[] {
            ReadlineLibrary.GnuReadline, ReadlineLibrary.Editline,
            ReadlineLibrary.Getline
        };
        for (ReadlineLibrary l: libs) {
            try {
                Readline.load(l);
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        Readline.cleanup();
                    }
                });
                return true;
            } catch (Throwable t) {}
        }
        return false;
    }

    private static void showHelp() {
        System.out
                .println("[Usage]:\tmysql/oracle host port database username password");
        System.out.println("Example:");
        System.out
                .println("\t\tmysql 192.168.201.78 9538 mydatabase myusername mypassword");
    }

    private static SQLClient makeClient(String type)
            throws ClassNotFoundException {
        SQLClient client = null;
        if ("mysql".equalsIgnoreCase(type)) {
            client = new MySQLClient();
        } else if ("oracle".equalsIgnoreCase(type)) {
            client = new OracleClient();
        } else {
            throw new IllegalArgumentException("unknow db type: " + type);
        }
        client.loadDriver();
        return client;
    }

    private static void exit() {
        System.exit(0);
    }

    private static void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}