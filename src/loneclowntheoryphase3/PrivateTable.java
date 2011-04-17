package loneclowntheoryphase3;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Neutron007
 */
public class PrivateTable
{
    private String dbms;
    private String dbName;
    private Connection con;

    public PrivateTable(Connection connArg, String dbmsArg, String dbNameArg)
    {
        super();
        this.con = connArg;
        this.dbms = dbmsArg;
        this.dbName = dbNameArg;
        try
        {
            String query = "USE " + dbName;
            Statement stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            stmt.execute(query);
            con.setAutoCommit(true);

        }
        catch (SQLException e)
        {
            System.out.println(e);
        }
    }

    public void createPrivateTable()
    {
        Statement stmt = null;
        String query = "";
        ResultSet pTable;

        try
        {
            stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            query = "SELECT '' AS ProductID, Price AS Price, DeptID AS DeptID, Weight AS Weight, ProductYear AS ProductYear, ExpireYear AS ExpireYear FROM LCTPhaseThree.Student ORDER BY Price, DeptID, Weight";

            pTable = stmt.executeQuery(query);

            System.out.println("PRIVATE TABLE");
            System.out.println("_______________________________________________________________________________________");
            System.out.println("ProductID\tPrice\tDeptID\tWeight\tProductYear\tExpireYear");
            while (pTable.next())
            {
                System.out.print(pTable.getString("ProductID"));
                System.out.print("\t\t " + pTable.getString("Price"));
                System.out.print("\t" + pTable.getString("DeptID"));
                System.out.print("\t\t" + pTable.getString("Weight"));
                System.out.print("\t  " + pTable.getString("ProductYear"));
                System.out.println("\t\t" + pTable.getString("ExpireYear"));
            }
            System.out.println("_______________________________________________________________________________________");
            System.out.println();
        }
        catch (SQLException e)
        {
            System.out.println(e);
        }
    }
}
