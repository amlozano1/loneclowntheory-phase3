/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package loneclowntheoryphase3;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Brian Arvidson
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        RandomFill toFill;
        PrivateTable privateTable;
        Connection con = null;

        String connStr = "jdbc:mysql://localhost:3306";
        String user = "root";
        String pwd = "root";
        String dbms = "mysql";
        String dbName = "LCTPhaseThree";

        try
        {
           
            con = DriverManager.getConnection(connStr, user, pwd);
           
            toFill = new RandomFill(con, dbms, dbName);
            privateTable = new PrivateTable(con, dbms, dbName);

            toFill.emptyTable();

            toFill.fillStudentTabel(25);
         
            toFill.printStudentTable();

            privateTable.createPrivateTable();
            con.close();
        }
        catch (SQLException e)
        {
            System.out.println(e);
        }
    }

}
