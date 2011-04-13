/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package loneclowntheoryphase3;
import java.util.Random;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Brian Arvidson
 */
public class RandomFill
{

    private String dbms;
    private String dbName;
    private Connection con;
    private String ranProductID;
    private int ranPrice;
    private int ranDeptID;
    private int ranWeight;
    private String ranProductYear;
    private String ranExpireYear;


    public RandomFill(Connection connArg, String dbmsArg, String dbNameArg)
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

    public void fillStudentTabel(int numberToFill)
    {
        String queryUnique = "";
        Random randomGen = new Random();
        Statement stmt = null;
        String query = "";
        ResultSet ensureUnique;

        try
        {
            stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);


            for(int i = 1; i <= numberToFill; i++)
            {
                ranProductID = Integer.toString(randomGen.nextInt(9999));
               // System.out.println("Java ProductID:" + ranProductID);
                queryUnique = "SELECT ProductID FROM LCTPhaseThree.Student WHERE ProductID = '" + ranProductID +"'";
                ensureUnique = stmt.executeQuery(queryUnique);

                if(ranProductID.length() == 1)
                {
                    ranProductID = "000" + ranProductID;
                }
                if(ranProductID.length() == 2)
                {
                    ranProductID = "00" + ranProductID;
                }
                if(ranProductID.length() == 3)
                {
                    ranProductID = "0" + ranProductID;
                }
                //System.out.println(ranProductID);

                ensureUnique = stmt.executeQuery("SELECT ProductID AS ProductID FROM " + dbName + "." + "Student");

                //Ensures that the ProductID is unique, if it is not unique, ProductID is changed untill a unique value is found.
                while(ensureUnique.next())
                {
                    if(((ensureUnique.getString("ProductID")).equals(ranProductID)))
                    {
                        System.out.print("ProductID " + ranProductID + " is not unique. Changing to ");
                        ranProductID = Integer.toString(randomGen.nextInt(9999));
                        if(ranProductID.length() == 1)
                        {
                            ranProductID = "000" + ranProductID;
                        }
                        if(ranProductID.length() == 2)
                        {
                            ranProductID = "00" + ranProductID;
                        }
                        if(ranProductID.length() == 3)
                        {
                            ranProductID = "0" + ranProductID;
                        }
                        System.out.println(ranProductID);
                    }
                }

                ranPrice = randomGen.nextInt(99999);

//                System.out.println("Row " + i + " ranPrice=" + ranPrice);

                ranDeptID = randomGen.nextInt(50);

//                System.out.println("Row " + i + " ranDeptID=" + ranDeptID);

                ranWeight = randomGen.nextInt(9);

//                System.out.println("Row " + i + " ranWeight=" + ranWeight);

                //Formula to specify a range m - n is nextint((n-m) + 1) + m
                //In this case m = 1980, n = 2010 formula is nextint(31) + 1980
                ranProductYear = Integer.toString((randomGen.nextInt(31) + 1980));

//                System.out.println("Row " + i + " ranProductYear=" + ranProductYear);

                //Formula to specify a range m - n is nextint((n-m) + 1) + m
                //The assignment documentation was not exact in what startdate is so I am assuming startdate is
                //the value from ProductYear so m = ProductYear, n = 2015 formula is nextint((2015 - ranProductYear) + 1) + ranProductYear
                ranExpireYear = Integer.toString((randomGen.nextInt((2015 - Integer.parseInt(ranProductYear)) + 1) + Integer.parseInt(ranProductYear)));

//                System.out.println("Row " + i + " ranExpireYear=" + ranExpireYear);

                query = "INSERT INTO " + dbName + "." + "Student"
                        + " (`ProductID`, `Price`, `DeptID`, `Weight`, `ProductYear`, `ExpireYear`) "
                        + "VALUES ('" + ranProductID + "','" + ranPrice + "','" + ranDeptID + "','" + ranWeight + "','" + ranProductYear + "','" + ranExpireYear + "')";
                ensureUnique = stmt.executeQuery(queryUnique);
                stmt.executeUpdate(query);

            }

//                System.out.println("OK");

                stmt.close();
        }
        catch (SQLException e)
        {
            System.out.println(e);
        }

    }

    public void emptyTable()
    {
        String deleteQuery = "";
        Statement stmt = null;
        try
        {
            stmt = con.createStatement();
            deleteQuery = "DELETE FROM Student";
            int deleteRows = stmt.executeUpdate(deleteQuery);
            if(deleteRows > 0)
            {
                System.out.println("Deleted all rows form table");
            }
            else
            {
                System.out.println("Table already empty");
            }

            stmt.close();
        }
        catch (SQLException e)
        {
           System.out.println(e);
        }
    }

    public void printStudentTable()
    {
        ResultSet result;
        Statement stmt = null;
       // String format;
     
        try
        {
            stmt = con.createStatement();
            result = stmt.executeQuery("SELECT * FROM " + dbName + "." + "Student");

            System.out.println("STUDENT TABLE");
            System.out.println("_______________________________________________________________________________________");
            System.out.println("ProductID\tPrice\tDeptID\tWeight\tProductYear\tExpireYear");
            while (result.next())
            {
                System.out.print(result.getString("ProductID"));
                System.out.print("\t     " + result.getString("Price"));
                System.out.print("\t" + result.getString("DeptID"));
                System.out.print("\t\t" + result.getString("Weight"));
                System.out.print("\t  " + result.getString("ProductYear"));
                System.out.println("\t\t" + result.getString("ExpireYear"));
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
