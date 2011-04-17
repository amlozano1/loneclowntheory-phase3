package loneclowntheoryphase3;

import java.sql.*;
import java.util.*;

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
    protected final String RESULT_TBL = "Result";
    protected final String DV_TBL = "DVTable";
    private final int PRICE_GROUPING_BASE = 10;
    private final int DEPT_ID_GROUPING_BASE = 2;
    private final int WEIGHT_GROUPING_BASE = 2;
    // Data members
    protected Connection con;
    protected ResultSet privateTable;
    protected ResultSet outliers;
    protected ResultSetMetaData privateTableMetaData;
    protected ResultSetMetaData outliersMetaData;
    protected int privateTableCount;
    protected int outlierCount;
    protected String[] QI;
    protected int k;
    protected int maxsup;
    protected int[][][] dvtable;
    protected int maxHeight = 0;
    protected ArrayList<int[]> possibleSolutions;
    protected ArrayList<ArrayList<String>> suppressionList;

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

    public void kanon(String[] QI, int k, int maxsup)
    {
        this.QI = QI;
        this.k = k;
        this.maxsup = maxsup;

        this.setPrivateTable(QI);
        this.setOutliers(QI, k);
        this.createDVTable();
        this.possibleSolutions = this.findSolution();
        for (int i = 0; i < this.possibleSolutions.size(); i++)
        {
            System.out.println(this.getDVString(this.possibleSolutions.get(i)));
            this.generalizePT(this.possibleSolutions.get(i), i);
        }
    }

    public void generalizePT(int[] dv, int newTableID)
    {
        String query = "";
        Statement stmt = null;

        try
        {
            this.privateTable.beforeFirst();

            this.copyTable(RESULT_TBL + newTableID, RESULT_TBL);

            while (this.privateTable.next())
            {
                String rowStr = "";

                if (!this.suppressionList.get(newTableID).contains(this.privateTable.getString("ProductID")))
                {
                    for (int i = 1; i <= this.privateTableMetaData.getColumnCount(); i++)
                    {
                        if (i < this.privateTableMetaData.getColumnCount())
                        {
                            int QIIndex = this.inQI(this.privateTableMetaData.getColumnName(i));

                            if (QIIndex >= 0)
                            {
                                rowStr = rowStr + "'" + this.genAttr(this.privateTable.getString(i), this.privateTableMetaData.getColumnName(i), dv[QIIndex]) + "',";
                            }
                            else
                            {
                                rowStr = rowStr + "'" + this.privateTable.getString(i) + "',";
                            }
                        }
                        else
                        {
                            int QIIndex = this.inQI(this.privateTableMetaData.getColumnName(i));

                            if (QIIndex >= 0)
                            {
                                rowStr = rowStr + "'" + this.genAttr(this.privateTable.getString(i), this.privateTableMetaData.getColumnName(i), dv[QIIndex]) + "'";
                            }
                            else
                            {
                                rowStr = rowStr + "'" + this.privateTable.getString(i) + "'";
                            }
                        }
                    }
                }
                else
                {
                    System.out.println("Supressing: " + this.privateTable.getString("ProductID"));
                    rowStr = "'--','--','--','--','--','--'";
                }

                query = "INSERT INTO " + RESULT_TBL + newTableID + " VALUES (" + rowStr + ");";

                stmt = con.createStatement();
                stmt.execute(query);
            }
        }
        catch (Exception e)
        {
            System.out.println("In generalizePT: " + e);
        }
    }

    protected int inQI(String str)
    {
        for (int i = 0; i < QI.length; i++)
        {
            if (str.equals(QI[i]))
            {
                return i;
            }
        }

        return -1;
    }

    public ArrayList<int[]> findSolution()
    {
        ArrayList<int[]> currSolutions = new ArrayList<int[]>();
        ArrayList<int[]> prevSolutions = new ArrayList<int[]>();
        ArrayList<String> suppressTuples = new ArrayList<String>();

        this.suppressionList = new ArrayList<ArrayList<String>>();

        String query = "";
        Statement stmt = null;
        ResultSet rs = null;

        int numOutliers = 0;
        int prevMax = this.maxHeight;
        int prevMin = 0;

        int height = prevMax / 2;
        int prevHeight = 0;
        int prevSolnHeight = this.maxHeight;

        try
        {
            while (height != prevHeight)
            {
                query = "SELECT DISTINCT dv FROM " + this.DV_TBL + " WHERE height = " + height + ";";
                stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                rs = stmt.executeQuery(query);

                while (rs.next())
                {
                    numOutliers = 0;
                    suppressTuples.clear();
                    int rowCount = 0;

                    int[] testDV = this.getDVArray(rs.getString("dv"));

                    for (int i = 0; i < this.outlierCount; i++)
                    {
                        rowCount = 0;

                        for (int j = 0; j < this.privateTableCount; j++)
                        {
                            if (this.dominates(this.dvtable[i][j], testDV))
                            {
                                rowCount++;
                            }
                        }

                        if (rowCount < this.k)
                        {
                            numOutliers++;
                            suppressTuples.add(this.getSuppressID(i));
                        }
                    }

                    if (numOutliers <= this.maxsup)
                    {
                        currSolutions.add(testDV);

                        if (height < prevSolnHeight)
                        {
                            this.suppressionList.clear();
                            this.suppressionList.add(new ArrayList<String>(suppressTuples));
                            prevSolnHeight = height;
                        }
                        else
                        {
                            this.suppressionList.add(new ArrayList<String>(suppressTuples));
                        }
                    }
                    else
                    {
                    }
                }

                if (currSolutions.size() > 0)
                {
                    prevSolutions.clear();
                    prevSolutions.addAll(currSolutions);
                    currSolutions.clear();

                    prevSolnHeight = height;
                    prevMax = height;
                    prevHeight = height;
                    height = (height + prevMin) / 2;
                }
                else
                {
                    prevMin = height;
                    prevHeight = height;
                    height = (height + prevMax) / 2;
                }

                if ((height == prevHeight) && prevSolutions.isEmpty())
                {
                    height = this.maxHeight;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("In findSolution: " + e);
        }

        return prevSolutions;
    }

    protected String getSuppressID(int rowID)
    {
        int row = 1;

        try
        {
            this.outliers.beforeFirst();

            while (this.outliers.next())
            {
                if ((rowID + 1) == row)
                {
                    return this.outliers.getString("ProductID");
                }
                else
                {
                    row++;
                }
            }

            this.outliers.beforeFirst();
        }
        catch (Exception e)
        {
            System.out.println("In getSuppressID " + e);
        }

        return null;
    }

    protected boolean dominates(int[] dv1, int[] dv2)
    {
        boolean dom = true;

        for (int i = 0; i < dv1.length; i++)
        {
            if (dv1[i] <= dv2[i])
            {
                dom = true;
            }
            else
            {
                return false;
            }
        }

        return dom;
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
        int height = 0;

        this.dvtable = new int[this.outlierCount][this.privateTableCount][];

        try
        {
            this.outliers.beforeFirst();
            this.privateTable.beforeFirst();

            int i = 0;

            while (this.outliers.next())
            {
                int j = 0;

                while (this.privateTable.next())
                {
                    dv = this.getDV(this.outliers, this.privateTable);
                    height = this.getHeight(dv);

                    if (height > this.maxHeight)
                    {
                        this.maxHeight = height;
                    }

                    this.dvtable[i][j] = dv;

                    query = "INSERT INTO " + this.DV_TBL
                            + " VALUES ('"
                            + this.outliers.getString("ProductID") + "','"
                            + this.privateTable.getString("ProductID") + "','"
                            + this.getDVString(dv) + "','"
                            + height + "');";
                    stmt = con.createStatement();
                    stmt.execute(query);
                    j++;
                }

                i++;
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
        int dv[];

        try
        {
            dv = new int[this.QI.length];

            for (int i = 0; i < QI.length; i++)
            {
                dv[i] = this.getDistance(this.outliers.getString(QI[i]), this.privateTable.getString(QI[i]), QI[i]);
            }

            return dv;
        }
        catch (Exception e)
        {
            System.out.println("In getDV: " + e);
            return null;
        }
    }

    //calculate distance between two QI
    public int getDistance(String str1, String str2, String attrName)
    {
        int distance = 0;

        if (attrName.equals("ProductID"))
        {
            distance = getStringDistance(str1, str2);
        }
        else if (attrName.equals("Price"))
        {
            distance = getIntDistance(Integer.parseInt(str1), Integer.parseInt(str2),
                    PRICE_GROUPING_BASE);
        }
        else if (attrName.equals("DeptID"))
        {
            distance = getIntDistance(Integer.parseInt(str1), Integer.parseInt(str2),
                    DEPT_ID_GROUPING_BASE);
        }
        else if (attrName.equals("Weight"))
        {
            distance = getIntDistance(Integer.parseInt(str1), Integer.parseInt(str2),
                    WEIGHT_GROUPING_BASE);
        }
        else if (attrName.equals("ProductYear"))
        {
            distance = getStringDistance(str1, str2);
        }
        else if (attrName.equals("ExpireYear"))
        {
            distance = getStringDistance(str1, str2);
        }
        else
        {
            distance = -1;
        }

        return distance;
    }

    //calculate distance between Strings (years and IDs)
    //find the first differing char and subtract its index from the length
    //that character and all before it need to be sanitized, so the difference is
    //also the distance
    public int getStringDistance(String str1, String str2)
    {
        final int ID_AND_YEAR_LENGTH = 4;

        //the number of characters that are the same
        //from left to right
        int numSameChars = 0;

        //find the number of characters that are the same, from the left to the
        //right
        while (numSameChars < ID_AND_YEAR_LENGTH
                && str1.charAt(numSameChars) == str2.charAt(numSameChars))
        {
            numSameChars++;
        }

        //return the number of characters we need to hide
        return ID_AND_YEAR_LENGTH - numSameChars;
    }

    //distance is what power of base (base^distance is the number of elements in
    //each set you must group the range of possible values into before first and
    //second are contained in the same group) you have to group by to get these
    //ints into the same group keep dividing until both equal 0 and we'll know
    //how many powers of radix they differ by
    public int getIntDistance(int price1, int price2, int base)
    {
        //if they're the same number initially, they're in a set of the size
        //base^0 (that's 1 if you need some help)
        int distance = 0;

        //so find the power
        while (price1 != price2)
        {
            distance++; //next power of base
            price1 /= base; //lop off a power of base
            price2 /= base; //lop off a power of base
        }

        return distance;
    }

    public String genAttr(String str, String attrName, int distance)
    {
        if (attrName.equals("ProductID"))
        {
            return this.generalizeString(str, distance);
        }
        else if (attrName.equals("Price"))
        {
            return this.generalizePrice(Integer.parseInt(str), distance);
        }
        else if (attrName.equals("DeptID"))
        {
            return this.generalizeDeptID(Integer.parseInt(str), distance);
        }
        else if (attrName.equals("Weight"))
        {
            return this.generalizeWeight(Integer.parseInt(str), distance);
        }
        else if (attrName.equals("ProductYear"))
        {
            return this.generalizeYears(str, distance);
        }
        else if (attrName.equals("ExpireYear"))
        {
            return this.generalizeYears(str, distance);
        }
        else
        {
            return "error";
        }
    }

    public int getHeight(int[] dv)
    {
        int height = 0;

        for (int i = 0; i < dv.length; i++)
        {
            height = height + dv[i];
        }

        return height;
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
//            query = "SELECT ProductID ," + quasiIdentList + ", count(*) as Count "
//                    + "FROM " + this.CPY_STUDENT_TBL + " "
//                    + "GROUP BY " + quasiIdentList + ";";
            query = "SELECT * "
                    + "FROM " + this.CPY_STUDENT_TBL + ";";
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
//            query = "SELECT ProductID ," + quasiIdentList + ", count(*) as Count "
//                    + "FROM " + this.CPY_STUDENT_TBL + " "
//                    + "GROUP BY " + quasiIdentList + " "
//                    + "HAVING Count < " + k + ";";
            query = "SELECT *, count(*) as Count "
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

    protected int[] getDVArray(String dvStr)
    {
        int[] result;

        if (dvStr.length() == 1)
        {
            result = new int[1];
            result[0] = Integer.parseInt(dvStr);
        }
        else
        {
            String[] strArray = dvStr.split(",");
            result = new int[strArray.length];

            for (int i = 0; i < strArray.length; i++)
            {
                result[i] = Integer.parseInt(strArray[i]);
            }
        }

        return result;
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

    /** generalizes in ranges that are powers of two
     * */
    public String generalizeInPowersOfTwo(int generlizee, int genAmount)
    {
        if (genAmount == 0)
        {
            return Integer.toString(generlizee);
        }
        int beginRange = 0;
        int endRange = 0;
        int range = 1 << genAmount;//find the range for this generalization, IE the spread between each generlization
        beginRange = generlizee / (range) * range;//get the lower bound by generalizee DIV range (integer divisions) times range
        endRange = beginRange + range - 1;//add one range to get the end range.
        return "<" + beginRange + "-" + endRange + ">";
    }

    //Generalizes the DeptID cloumn.  There are 0-6 possible levels. Level 0 is no generalization. Level 1 generalization a range of 2.
    //Level 2 generalization a range of 4. Level 3 generalization a range of 8. Level 4 generalization a range of 16. Level 5 generalization a range of 32.
    //Level 6 generalization a range of 64.
    public String generalizeDeptID(int deptID, int genAmount)
    {
        return generalizeInPowersOfTwo(deptID, genAmount);
    }

    public String generalizeWeight(int weight, int genAmount)
    {
        return generalizeInPowersOfTwo(weight, genAmount);
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
        if (genAmount == 3)
        {
            for (int i = 3; i > 0; i--)
            {
                genString[i] = '*';
            }
        }
        if (genAmount == 4)
        {
            for (int i = 3; i >= 0; i--)
            {
                genString[i] = '*';
            }
        }
        result = new String(genString);

        return result;
    }

    public void printDVTable()
    {
        for (int i = 0; i < this.dvtable.length; i++)
        {
            for (int j = 0; j < this.dvtable[i].length; j++)
            {
                System.out.print("[");
                for (int k = 0; k < this.dvtable[i][j].length; k++)
                {
                    System.out.print(this.dvtable[i][j][k] + ",");
                }
                System.out.print("]\t");
            }
            System.out.println();
        }
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
