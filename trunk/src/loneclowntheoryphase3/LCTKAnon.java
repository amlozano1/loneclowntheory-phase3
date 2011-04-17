/**
 * Lone Clown Theory Phase 3
 *
 * Brandon Andersen
 * Brian Arvidson
 * Anthony Lozano
 * Justin Paglierani
 *
 * CSE 467/598
 * Spring 2011
 * Prof. Ahn
 *
 * LCTKAnon
 */
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
    protected final String DV_TBL_COPY = "DVTable_Copy";
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

    /**
     * Default constructor to initialize the class
     */
    public LCTKAnon()
    {
        super();

        // setup db connection
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

    /**
     * Accepts a String[] containing the quasi-indent list, an int k for k-anonymity,
     * and an int maxsup for maximum allowed tuple suppression
     *
     * The method implements Samarati's algorithm for k-anonymity
     *
     * @param QI
     * @param k
     * @param maxsup
     */
    public void kanon(String[] QI, int k, int maxsup)
    {
        // save to class data members
        this.QI = QI;
        this.k = k;
        this.maxsup = maxsup;

        this.setPrivateTable(); // get the private table (acts as the list of all tuples)
        this.setOutliers(QI, k); // get the outliers based on given k
        this.createDVTable(); // build the DV matrix
        this.possibleSolutions = this.findSolution(); // find the minimal solution(s)

        // for each minimal solution, create a generalized version of the private table
        for (int i = 0; i < this.possibleSolutions.size(); i++)
        {
            System.out.println(this.getDVString(this.possibleSolutions.get(i)));
            this.generalizePT(this.possibleSolutions.get(i), i);
        }
    }

    /**
     * This method takes a distance vector solution and creates a generalized table
     * in the database
     *
     * The new table in the database will be called 'result0', 'result1', etc...
     * depending on the number of minimal solutions found
     *
     * Also, the result table will be a modified version of the 'student' table
     * in that all attributes are of type VARCHAR to support expressing generalization
     * in terms of ranges for what were originally integer values
     *
     * @param dv
     * @param newTableID
     */
    public void generalizePT(int[] dv, int newTableID)
    {
        String query = "";
        Statement stmt = null;

        try
        {
            this.privateTable.beforeFirst(); // ensure we are at the beginning of the pt rs

            this.copyTable(RESULT_TBL + newTableID, RESULT_TBL); // make a copy of the result table in the db

            // loop through the private table
            while (this.privateTable.next())
            {
                String rowStr = "";

                // check to see if this tuple will be suppressed
                if (!this.suppressionList.get(newTableID).contains(this.privateTable.getString("ProductID")))
                {
                    // not suppressed, therefore build the qry string, generalize attributes in the QI
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
                    // suppressing tuple so make it evident in results by entering '--' for each attribute
                    System.out.println("Supressing: " + this.privateTable.getString("ProductID"));
                    rowStr = "'--','--','--','--','--','--'";
                }

                // final query string
                query = "INSERT INTO " + RESULT_TBL + newTableID + " VALUES (" + rowStr + ");";

                // execute the query
                stmt = con.createStatement();
                stmt.execute(query);
            }
        }
        catch (Exception e)
        {
            System.out.println("In generalizePT: " + e);
        }
    }

    /**
     * Helper method to the index of an attribute from the QI
     *
     * @param str
     * @return
     */
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

    /**
     * This method finds the minimal solution(s)
     *
     * It returns an ArrayList of Distance Vectors that satisfy k-anonymity and minimality
     *
     * It also determines what tuples can be suppressed for a given solution
     *
     * @return
     */
    public ArrayList<int[]> findSolution()
    {
        // ArraryLists for storing varying sizes of stuff...
        ArrayList<int[]> currSolutions = new ArrayList<int[]>();
        ArrayList<int[]> prevSolutions = new ArrayList<int[]>();
        ArrayList<String> suppressTuples = new ArrayList<String>();

        // Instantiate the suppression list, an ArrayList of ArrayLists...
        this.suppressionList = new ArrayList<ArrayList<String>>();

        String query = "";
        Statement stmt = null;
        ResultSet rs = null;

        // variables to track our status
        int numOutliers = 0;
        int prevMax = this.maxHeight;
        int prevMin = 0;
        // start at the max height found div 2 (integer arith so it works like floor fxn)
        int height = prevMax / 2;
        int prevHeight = 0;
        int prevSolnHeight = this.maxHeight;

        try
        {
            // perform a binary search through the dv table to find minimal solutions
            while (height != prevHeight)
            {
                // get the possible dv's found at height from the db
                query = "SELECT DISTINCT dv FROM " + this.DV_TBL_COPY + " WHERE height = " + height + ";";
                stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                rs = stmt.executeQuery(query);

                // loop through the results
                while (rs.next())
                {
                    // init outliers and suppression candidates
                    numOutliers = 0;
                    suppressTuples.clear();
                    int rowCount = 0;

                    // get the first dv to try at height
                    int[] testDV = this.getDVArray(rs.getString("dv"));

                    // loop through the dvtable (matrix) and count the dv's that are
                    // <= the testDV
                    for (int i = 0; i < this.outlierCount; i++)
                    {
                        rowCount = 0;

                        for (int j = 0; j < this.privateTableCount; j++)
                        {
                            // check to see if current dv <= testDV
                            if (this.dominates(this.dvtable[i][j], testDV))
                            {
                                // if so, count it
                                rowCount++;
                            }
                        }

                        // if the row count came up less than k, this might be
                        // a suppression candidate
                        if (rowCount < this.k)
                        {
                            numOutliers++;
                            suppressTuples.add(this.getSuppressID(i));
                        }
                    }

                    // if the total outliers (suppr. cand.) is less then the
                    // max allowed suppression, then the testDV is a potential
                    // solution, and the suppr. cand. are also tracked
                    if (numOutliers <= this.maxsup)
                    {
                        currSolutions.add(testDV);

                        // check to see if we have found a new solution at a lower level
                        // than the previous found
                        if (height < prevSolnHeight)
                        {
                            // if so, clear out any previous suppression lists
                            this.suppressionList.clear();
                            // add the new suppression list
                            this.suppressionList.add(new ArrayList<String>(suppressTuples));
                            // note the new solution height
                            prevSolnHeight = height;
                        }
                        else
                        {
                            // already found a solution at this height so don't
                            // clear the suppression list
                            this.suppressionList.add(new ArrayList<String>(suppressTuples));
                        }
                    }
                }

                // see if we found any solutions at this height
                if (currSolutions.size() > 0)
                {
                    // if so store them in the previous solution list
                    // so we can try at a new height
                    prevSolutions.clear();
                    prevSolutions.addAll(currSolutions);
                    currSolutions.clear();

                    // do some math to determine the new height to search at
                    // since we found a solution here, try at a lower height
                    // be half-splitting
                    prevSolnHeight = height;
                    prevMax = height;
                    prevHeight = height;
                    height = (height + prevMin) / 2;
                }
                else
                {
                    // didn't find a solution at this height so move up,
                    // again half-splitting the distance
                    prevMin = height;
                    prevHeight = height;
                    height = (height + prevMax) / 2;
                }

                // final check for the case where we have not found any solution
                // and no new height is calculated, this means the solution is
                // at the top of the lattice
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

        // return the minimal solution(s)
        return prevSolutions;
    }

    /**
     * Helper method to get a ProductID for a tuple to suppress
     * Used for adding candidate tuples to suppress, identifying them
     * by their unique ProductID
     *
     * @param rowID
     * @return
     */
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

    /**
     * Checks the partial ordering between two dv's and returns true if the
     * 1st dv <= 2nd dv
     *
     * @param dv1
     * @param dv2
     * @return
     */
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

    /**
     * Helper method to copy a table, drops any instance of the first, then
     * copies only structure from the second to the first, then inserts all
     * the second into the first
     *
     * @param newTable
     * @param oldTable
     */
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

    /**
     * Clears out the DV_TBL_COPY for another round without having to re-run the
     * sql batch file
     *
     */
    public void refreshDVTable()
    {
        Statement stmt = null;
        String dropQry = "DROP TABLE IF EXISTS " + this.DV_TBL_COPY + ";";
        String createQry = "CREATE TABLE " + this.DV_TBL_COPY + " LIKE " + this.DV_TBL + ";";

        try
        {
            stmt = con.createStatement();
            stmt.execute(dropQry);
            stmt.execute(createQry);
        }
        catch (Exception e)
        {
            System.out.println("In copyTable: " + e);
        }
    }

    /**
     * Creates the dvtable (both the matrix stored in class and db version)
     * The in-class matrix version is the one used for counting, while the
     * db version is just to find candidate dv's at a height given during the
     * search (just provides easy lookup)
     *
     */
    public void createDVTable()
    {
        Statement stmt = null;
        String query = "";
        int[] dv;
        int height = 0;

        // instantiate the dvtable matrix
        this.dvtable = new int[this.outlierCount][this.privateTableCount][];

        try
        {
            // rewind the result sets
            this.outliers.beforeFirst();
            this.privateTable.beforeFirst();

            int i = 0;

            // loop through the outliers
            while (this.outliers.next())
            {
                int j = 0;

                // for each entry in the pt, calculate a dv between it
                // and the current outlier
                while (this.privateTable.next())
                {
                    // get the dv
                    dv = this.getDV(this.outliers, this.privateTable);
                    // calculate its height for easy db lookup
                    height = this.getHeight(dv);

                    // make a note of the max height found
                    if (height > this.maxHeight)
                    {
                        this.maxHeight = height;
                    }

                    // enter the dv into the matrix
                    this.dvtable[i][j] = dv;

                    // also, insert into the db with its height
                    query = "INSERT INTO " + this.DV_TBL_COPY
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

    /**
     * This method calculates the distance vector between to rows based on the
     * QI list, returning an int[] representing the dv
     *
     * @param rs1
     * @param rs2
     * @return
     */
    public int[] getDV(ResultSet rs1, ResultSet rs2)
    {
        int dv[];

        try
        {
            // make a new dv same len as QI
            dv = new int[this.QI.length];

            // for each attrib in QI, calc the distance
            for (int i = 0; i < QI.length; i++)
            {
                dv[i] = this.getDistance(rs1.getString(QI[i]), rs2.getString(QI[i]), QI[i]);
            }

            // return the dv
            return dv;
        }
        catch (Exception e)
        {
            System.out.println("In getDV: " + e);
            return null;
        }
    }

    /**
     * calculate distance between two QI strings
     *
     * @param str1
     * @param str2
     * @param attrName
     * @return
     */
    public int getDistance(String str1, String str2, String attrName)
    {
        int distance = 0;

        // call the appropriate distance fxn depending on which attrib it is
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

    /**
     * Generalizes a given attrib the required number of steps using assoc.
     * helper functions depending on the QI attrib name
     *
     * @param str
     * @param attrName
     * @param distance
     * @return
     */
    public String genAttr(String str, String attrName, int distance)
    {
        // Call the appropriate generalize fxn
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

    /**
     * Helper method to calculate the height of a dv
     * @param dv
     * @return
     */
    public int getHeight(int[] dv)
    {
        int height = 0;

        for (int i = 0; i < dv.length; i++)
        {
            height = height + dv[i];
        }

        return height;
    }

    /**
     * Creates the private table result set
     *
     */
    public void setPrivateTable()
    {
        Statement stmt = null;
        String query = "";

        try
        {
            stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
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

    /**
     * Creates the outlier result set based on the QI list and k level to anonymize to
     *
     * @param QI
     * @param k
     */
    public void setOutliers(String[] QI, int k)
    {
        Statement stmt = null;
        String query = "";
        String quasiIdentList = this.getQIString(QI);

        try
        {
            stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
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

    /**
     * Utility method to troubleshoot
     *
     * @param QI
     */
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

    /**
     * Utility method to troubleshoot
     *
     * @param QI
     */
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

    /**
     * Helper method to create a comma delimited string of outliers for
     * use in queries
     *
     * @param QI
     * @return
     */
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

    /**
     * Helper method for creating a comma delimited dv string
     *
     * @param dv
     * @return
     */
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

    /**
     * Helper method to create a dv (int[]) from a comma separated dv string
     *
     * @param dvStr
     * @return
     */
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

    /**
     * Count up the rows in a result set
     *
     * @param rs
     * @return
     */
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

    /**
     * Utility method for troubleshooting
     *
     */
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

    /**
     * Main method which can be run independently, must have a filled out copy
     * of the student table in the mysql db
     *
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            // instantiate the class
            LCTKAnon lct = new LCTKAnon();

            // copy the student table
            lct.copyTable(lct.CPY_STUDENT_TBL, lct.STUDENT_TBL);
            // clear out the dv table copy to allow running multiple times
            // on same student dataset
            lct.refreshDVTable();

            // use these to change up your QI list entries
            //
            // "ProductID", "Price", "DeptID", "Weight", "ProductYear", "ExpireYear"

            // create a QI list
            String[] QI =
            {
                "Price", "DeptID", "Weight"
            };

            // call kanon with the QI list, k-value, and maxsup
            lct.kanon(QI, 2, 5);
        }
        catch (Exception e)
        {
            System.out.println("In LCTKAnon main: " + e);
        }
    }
}
