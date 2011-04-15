package loneclowntheoryphase3;

import java.sql.*;

/**
 *
 * @author Brandon
 */
public class LCTKAnon
{
    // Constants

    protected final String dbms = "mysql";
    protected final String dbName = "LCTPhaseThree";
    protected final String connStr = "jdbc:mysql://localhost:3306";
    protected final String user = "root";
    protected final String pwd = "root";
    protected final String STUDENT_TBL = "Student";
    protected final String CPY_STUDENT_TBL = "Student_Copy";
    protected final String DV_TBL = "DVTable";
    // Data members
    protected Connection con;
    protected ResultSet privateTable;
    protected ResultSet outliers;
    protected ResultSetMetaData privateTableMetaData;
    protected ResultSetMetaData outliersMetaData;
    protected int privateTableCount;
    protected int outlierCount;

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

    public void copyTable(String newTable, String oldTable)
    {
        Statement stmt = null;
        String dropQry = "DROP TABLE IF EXISTS " + newTable + ";";
        String createQry = "CREATE TABLE " + newTable + " LIKE " + oldTable + ";";
        String selIntoQry = "INSERT INTO " + newTable + " SELECT * FROM " + oldTable + ";";

        try
        {
            stmt = con.createStatement();
            stmt.execute(dropQry);
            stmt.execute(createQry);
            stmt.execute(selIntoQry);
        }
        catch (Exception e)
        {
            System.out.println("In copyTable: " + e);
        }
    }

    public void createDVTable()
    {
        Statement stmt = null;
        String query = "";
        int[] dv;

        try
        {
            this.outliers.beforeFirst();
            this.privateTable.beforeFirst();

            while (this.outliers.next())
            {
                while (this.privateTable.next())
                {
                    // calculate dv and height
                    // insert dv and height into dvtable
                    if (!(this.outliers.getString("ProductID").equals(this.privateTable.getString("ProductID"))))
                    {
                        dv = this.getDV(this.outliers, this.privateTable);

                        query = "INSERT INTO " + this.DV_TBL
                                + " VALUES ('"
                                + this.outliers.getString("ProductID") + "','"
                                + this.privateTable.getString("ProductID") + "','"
                                + this.getDVString(dv) + "','"
                                + this.getHeight(dv) + "');";
                        stmt = con.createStatement();
                        stmt.execute(query);
                    }
                }

                this.privateTable.beforeFirst();
            }

            this.outliers.beforeFirst();
        }
        catch (Exception e)
        {
            System.out.println("In createDVTable: " + e);
        }
    }

    public int[] getDV(ResultSet rs1, ResultSet rs2)
    {
        return new int[] {1,2};
    }

    public String getHeight(int[] dv)
    {
        int height = 0;

        for (int i = 0; i < dv.length; i++)
        {
            height = height + dv[i];
        }
        return Integer.toString(height);
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
            query = "SELECT ProductID ," + quasiIdentList + ", count(*) as Count "
                    + "FROM " + this.CPY_STUDENT_TBL + " "
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
            query = "SELECT ProductID ," + quasiIdentList + ", count(*) as Count "
                    + "FROM " + this.CPY_STUDENT_TBL + " "
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

    protected String getQIString(String[] QI)
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

    protected String getDVString(int[] dv)
    {
        String dvString = "";

        for (int i = 0; i < dv.length; i++)
        {
            if (i == (dv.length - 1))
            {
                dvString = dvString + dv[i];
            }
            else
            {
                dvString = dvString + dv[i] + ",";
            }
        }

        return dvString;
    }

    protected int countRows(ResultSet rs)
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

    //Returns the generalized version of a String.  The value of genAmount will determin how many vlaues will be hidden by * starting with the right most letter.
    public String generalizeString(String anyString, int genAmount)
    {
        int index = anyString.length() - 1;
        char genString[] = anyString.toCharArray();

        //Check to make sure the amount to be generilzed in less then the length of the string being generilzed.
        if (genAmount <= anyString.length())
        {
            for (int i = genAmount; i > 0; i--)
            {
                //System.out.println("i=" + i + " index=" + index);
                genString[index] = '*';

                index--;
            }
        }

        String result = new String(genString);

        return result;
    }

    //Generalizes the Price column.  There are 0-5 levels.  Level 0 results in no generalization.
    //Level 1 results in a generalization of 10's.  Level 2 results in a generalization of 100's.
    //Level 3 results in the generalization of 1000's.  Level 4 results in the generalization of 10000's.
    //Level 5 results in the generalization of 100000's
    public String generalizePrice(int price, int genAmount)
    {
        String result = null;
        int beginRange = 0;
        int endRange = 0;

        if (genAmount == 0)
        {
            result = Integer.toString(price);
        }
        if (genAmount == 1)
        {
            if (price <= 10)
            {
                result = "<0-9>";
            }
            else
            {
                beginRange = price - (price % 10);
                endRange = beginRange + 9;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        if (genAmount == 2)
        {
            if (price <= 100)
            {
                result = "<0-99>";
            }
            else
            {
                beginRange = price - (price % 100);
                endRange = beginRange + 99;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        if (genAmount == 3)
        {
            if (price <= 1000)
            {
                result = "<0-999>";
            }
            else
            {
                beginRange = price - (price % 1000);
                endRange = beginRange + 999;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        if (genAmount == 4)
        {
            if (price <= 10000)
            {
                result = "<0-9999>";
            }
            else
            {
                beginRange = price - (price % 10000);
                endRange = beginRange + 9999;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        if (genAmount == 5)
        {
            if (price <= 100000)
            {
                result = "<0-99999>";
            }
            else
            {
                beginRange = price - (price % 100000);
                endRange = beginRange + 99999;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        return result;
    }

    //Generalizes the DeptID cloumn.  There are 0-6 possible levels. Level 0 is no generalization. Level 1 generalization a range of 2.
    //Level 2 generalization a range of 4. Level 3 generalization a range of 8. Level 4 generalization a range of 16. Level 5 generalization a range of 32.
    //Level 6 generalization a range of 64.
    public String generalizeDeptID(int deptID, int genAmount)
    {
        String result = null;
        int beginRange = 0;
        int endRange = 0;

        if (genAmount == 0)
        {
            result = Integer.toString(deptID);
        }
        if (genAmount == 1)
        {
            if (deptID == 0)
            {
                result = "<0-1>";
            }
            else
            {
                beginRange = deptID - 1;
                endRange = deptID;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        if (genAmount == 2)
        {
            if (deptID < 1)
            {
                result = "<0-3>";
            }
            else
            {
                beginRange = deptID - 1;
                endRange = deptID + 2;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        if (genAmount == 3)
        {
            if (deptID < 7)
            {
                result = "<0-7>";
            }
            else
            {
                beginRange = deptID - 5;
                endRange = deptID + 2;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        if (genAmount == 4)
        {
            if (deptID < 15)
            {
                result = "<0-15>";
            }
            else
            {
                beginRange = deptID - 8;
                endRange = deptID + 7;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        if (genAmount == 5)
        {
            if (deptID < 31)
            {
                result = "<0-32>";
            }
            else
            {
                beginRange = deptID - 16;
                endRange = deptID + 15;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        if (genAmount == 6)
        {
            result = "<0-50>";
        }


        return result;
    }

    public String generalizeWeight(int weight, int genAmount)
    {
        String result = null;
        int beginRange = 0;
        int endRange = 0;

        if (genAmount == 0)
        {
            result = Integer.toString(weight);
        }
        if (genAmount == 1)
        {
            if (weight == 0)
            {
                result = "<0-1>";
            }
            else
            {
                beginRange = weight - 1;
                endRange = weight;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        if (genAmount == 2)
        {
            if (weight <= 1)
            {
                result = "<0-3>";
            }
            if (weight == 9)
            {
                beginRange = weight - 3;
                endRange = weight;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
            if (weight == 8)
            {
                beginRange = weight - 2;
                endRange = weight + 1;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
            if (weight == 2 || weight == 3 || weight == 4 || weight == 5 || weight == 6 || weight == 7)
            {
                beginRange = weight - 1;
                endRange = weight + 2;
                result = ("<" + Integer.toString(beginRange) + "-" + Integer.toString(endRange) + ">");
            }
        }
        return result;
    }

    //Generalizes the ProductYear and ExpireYear columns. There are 0-2 levels possible.  Level 0 results in no generalization. Levle 1 results in the decaded being returned.
    //Level 2 results in the century being returned.
    public String generalizeYears(String year, int genAmount)
    {
        char genString[] = year.toCharArray();
        String decade = null;
        String result;

        //Check to make sure the amount to be generilzed in less then the length of the string being generilzed.
        if (genAmount == 1)
        {
            decade = (Character.toString(genString[2]) + "0" + "s");
            return decade;
        }
        if (genAmount == 2)
        {

            for (int i = 3; i > 1; i--)
            {
                genString[i] = '*';
            }
        }
        result = new String(genString);

        return result;
    }

//    public static void main(String[] args)
//    {
//        try
//        {
//
////            System.out.println("Running LCTKAnon");
////
////            LCTKAnon lct = new LCTKAnon();
////
//////            System.out.println("Generalization String Test 1234 by 0: " + lct.generalizeString("1234", 0));
//////            System.out.println("Generalization String Test 1234 by 1: " + lct.generalizeString("1234", 1));
//////            System.out.println("Generalization String Test 1234 by 2: " + lct.generalizeString("1234", 2));
//////            System.out.println("Generalization String Test 1234 by 3: " + lct.generalizeString("1234", 3));
//////            System.out.println("Generalization String Test 1234 by 4: " + lct.generalizeString("1234", 4));
//////
//////            System.out.println("Generalization Price Test 9 by 0: " + lct.generalizePrice(9, 0));
//////            System.out.println("Generalization Price Test 100 by 0: " + lct.generalizePrice(100, 0));
//////            System.out.println("Generalization Price Test 50 by 1: " + lct.generalizePrice(50, 1));
//////            System.out.println("Generalization Price Test 11 by 1: " + lct.generalizePrice(11, 1));
//////            System.out.println("Generalization Price Test 7 by 2: " + lct.generalizePrice(7, 2));
//////            System.out.println("Generalization Price Test 75 by 2: " + lct.generalizePrice(75, 2));
//////            System.out.println("Generalization Price Test 150 by 2: " + lct.generalizePrice(150, 2));
//////            System.out.println("Generalization Price Test 35 by 3: " + lct.generalizePrice(35, 3));
//////            System.out.println("Generalization Price Test 1150 by 3: " + lct.generalizePrice(1150, 3));
//////            System.out.println("Generalization Price Test 9999 by 4: " + lct.generalizePrice(9999, 4));
//////            System.out.println("Generalization Price Test 120 by 4: " + lct.generalizePrice(120, 4));
//////            System.out.println("Generalization Price Test 1073 by 4: " + lct.generalizePrice(1073, 4));
//////            System.out.println("Generalization Price Test 300 by 5: " + lct.generalizePrice(300, 5));
//////
//////            System.out.println("Generalization DeptID Test 13 by 0: " + lct.generalizeDeptID(13, 0));
//////            System.out.println("Generalization DeptID Test 13 by 1: " + lct.generalizeDeptID(13, 1));
//////            System.out.println("Generalization DeptID Test 13 by 2: " + lct.generalizeDeptID(13, 2));
//////            System.out.println("Generalization DeptID Test 13 by 3: " + lct.generalizeDeptID(13, 3));
//////            System.out.println("Generalization DeptID Test 15 by 4: " + lct.generalizeDeptID(15, 4));
//////            System.out.println("Generalization DeptID Test 15 by 5: " + lct.generalizeDeptID(15, 5));
//////            System.out.println("Generalization DeptID Test 40 by 5: " + lct.generalizeDeptID(40, 5));
//////            System.out.println("Generalization DeptID Test 40 by 6: " + lct.generalizeDeptID(40, 6));
//////
//////            System.out.println("Generalization Weight Test 5 by 0: " + lct.generalizeWeight(5, 0));
//////            System.out.println("Generalization Weight Test 5 by 1: " + lct.generalizeWeight(5, 1));
//////            System.out.println("Generalization Weight Test 5 by 2: " + lct.generalizeWeight(5, 2));
//////            System.out.println("Generalization Weight Test 0 by 1: " + lct.generalizeWeight(0, 1));
//////            System.out.println("Generalization Weight Test 9 by 2: " + lct.generalizeWeight(9, 2));
//////            System.out.println("Generalization Weight Test 8 by 1: " + lct.generalizeWeight(8, 1));
//////            System.out.println("Generalization Weight Test 6 by 2: " + lct.generalizeWeight(6, 2));
//////            System.out.println("Generalization Weight Test 7 by 2: " + lct.generalizeWeight(7, 2));
//////
//////            System.out.println("Generalization Years Test 1996 by 0: " + lct.generalizeYears("1996", 0));
//////            System.out.println("Generalization Years Test 1996 by 1: " + lct.generalizeYears("1996", 1));
//////            System.out.println("Generalization Years Test 1996 by 2: " + lct.generalizeYears("1996", 2));
//////            System.out.println("Generalization Years Test 1996 by 0: " + lct.generalizeYears("1985", 0));
//////            System.out.println("Generalization Years Test 1996 by 1: " + lct.generalizeYears("1985", 1));
//////            System.out.println("Generalization Years Test 1996 by 2: " + lct.generalizeYears("1985", 2));
//////            System.out.println("Generalization Years Test 1996 by 0: " + lct.generalizeYears("2001", 0));
//////            System.out.println("Generalization Years Test 1996 by 1: " + lct.generalizeYears("2001", 1));
//////            System.out.println("Generalization Years Test 1996 by 2: " + lct.generalizeYears("2001", 2));
////
////            lct.copyTable(lct.CPY_STUDENT_TBL, lct.STUDENT_TBL);
////
////            String[] QI =
////            {
////                "Weight", "ProductYear"
////            };
////
////            System.out.println("QI List: " + lct.getQIString(QI));
////
////            lct.setPrivateTable(QI);
////            lct.setOutliers(QI, 2);
////
////            System.out.println("\nPrinting PT\tRows: " + lct.privateTableCount + "\tColumns: " + lct.privateTableMetaData.getColumnCount() + "\n");
////            lct.printPrivateTable(QI);
////            System.out.println("\nPrinting Outliers\tRows: " + lct.outlierCount + "\tColumns: " + lct.outliersMetaData.getColumnCount() + "\n");
////            lct.printOutliers(QI);
////
////            lct.createDVTable();
//        }
//        catch (Exception e)
//        {
//            System.out.println("In LCTKAnon main: " + e);
//        }
//    }
}
