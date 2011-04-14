package loneclowntheoryphase3;

import java.sql.*;

/**
 *
 * @author Brandon
 */
public class LCTKAnon
{

    protected final String dbms = "mysql";
    protected final String dbName = "LCTPhaseThree";
    protected final String connStr = "jdbc:mysql://localhost:3306";
    protected final String user = "root";
    protected final String pwd = "root";
    protected Connection con;
    protected ResultSet privateTable;
    protected ResultSet outliers;
    protected ResultSetMetaData privateTableMetaData;
    protected ResultSetMetaData outliersMetaData;
    protected int privateTableCount;
    protected int outlierCount;
    protected int[][][] dvTable;

    public LCTKAnon()
    {
        super();

        try
        {
            con = DriverManager.getConnection(connStr, user, pwd);
            String query = "USE " + dbName;
            Statement stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            stmt.execute(query);
            con.setAutoCommit(true);
        }
        catch (Exception e)
        {
            System.out.println("In LCTKAnon Constructor: " + e);
        }
    }

    public void createDVTable()
    {
        Statement stmt = null;
        String query = "";

        try
        {
            
        }
        catch (Exception e)
        {
            System.out.println("In createDVTable: " + e);
        }
    }

    public void setPrivateTable(String[] QI)
    {
        Statement stmt = null;
        String query = "";
        String quasiIdentList = this.getQIString(QI);

        //SELECT quasiIdentList, count(*) as Count FROM `lctphasethree`.`student` GROUP BY quasiIdentList;
        try
        {
            stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            query = "SELECT ProductID as RowID," + quasiIdentList + ", count(*) as Count "
                    + "FROM LCTPhaseThree.Student "
                    + "GROUP BY " + quasiIdentList + ";";
            this.privateTable = stmt.executeQuery(query);
            this.privateTableMetaData = this.privateTable.getMetaData();
            this.privateTableCount = this.countRows(privateTable);
        }
        catch (Exception e)
        {
            System.out.println("In setPrivateTable: " + e);
        }
    }

    public void setOutliers(String[] QI, int k)
    {
        Statement stmt = null;
        String query = "";
        String quasiIdentList = this.getQIString(QI);

        //SELECT quasiIdentList, count(*) as Count FROM `lctphasethree`.`student` GROUP BY quasiIdentList HAVING Count < 2;
        try
        {
            stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            query = "SELECT ProductID as RowID," + quasiIdentList + ", count(*) as Count "
                    + "FROM LCTPhaseThree.Student "
                    + "GROUP BY " + quasiIdentList + " "
                    + "HAVING Count < " + k + ";";
            this.outliers = stmt.executeQuery(query);
            this.outliersMetaData = this.outliers.getMetaData();
            this.outlierCount = this.countRows(outliers);
        }
        catch (Exception e)
        {
            System.out.println("In setOutliers: " + e);
        }
    }

    public void printPrivateTable(String[] QI)
    {
        try
        {
            this.privateTable.beforeFirst();

            while (this.privateTable.next())
            {
                for (int i = 1; i <= this.privateTableMetaData.getColumnCount(); i++)
                {
                    System.out.print(this.privateTable.getString(i) + "\t");
                }

                System.out.println();
            }

            this.privateTable.beforeFirst();
        }
        catch (Exception e)
        {
            System.out.println("In printPrivateTable: " + e);
        }
    }

    public void printOutliers(String[] QI)
    {
        try
        {
            this.outliers.beforeFirst();

            while (this.outliers.next())
            {
                for (int i = 1; i <= this.outliersMetaData.getColumnCount(); i++)
                {
                    System.out.print(this.outliers.getString(i) + "\t");
                }

                System.out.println();
            }

            this.outliers.beforeFirst();
        }
        catch (Exception e)
        {
            System.out.println("In printOutliers: " + e);
        }
    }

    private String getQIString(String[] QI)
    {
        String quasiIdentList = "";

        for (int i = 0; i < QI.length; i++)
        {
            if (i == (QI.length - 1))
            {
                quasiIdentList = quasiIdentList + QI[i];
            }
            else
            {
                quasiIdentList = quasiIdentList + QI[i] + ",";
            }
        }

        return quasiIdentList;
    }

    private int countRows(ResultSet rs)
    {
        int count = 0;

        try
        {
            rs.beforeFirst();

            while (rs.next())
            {
                count++;
            }

            rs.beforeFirst();
        }
        catch (Exception e)
        {
            System.out.println("In countRows: " + e);
        }

        return count;
    }

    public static void main(String[] args)
    {
        try
        {
            System.out.println("Running LCTKAnon");

            LCTKAnon lct = new LCTKAnon();

            String[] QI =
            {
                "Weight", "ProductYear"
            };

            System.out.println("QI List: " + lct.getQIString(QI));

            lct.setPrivateTable(QI);
            lct.setOutliers(QI, 2);

            System.out.println("\nPrinting PT\tRows: " + lct.privateTableCount + "\tColumns: " + lct.privateTableMetaData.getColumnCount() + "\n");
            lct.printPrivateTable(QI);
            System.out.println("\nPrinting Outliers\tRows: " + lct.outlierCount + "\tColumns: " + lct.outliersMetaData.getColumnCount() + "\n");
            lct.printOutliers(QI);
        }
        catch (Exception e)
        {
            System.out.println("In LCTKAnon main: " + e);
        }
    }
}
