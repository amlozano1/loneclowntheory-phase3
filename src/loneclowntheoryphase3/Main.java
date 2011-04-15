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
public class Main
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
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

            System.out.println("Running LCTKAnon");

            LCTKAnon lct = new LCTKAnon();

//            System.out.println("Generalization String Test 1234 by 0: " + lct.generalizeString("1234", 0));
//            System.out.println("Generalization String Test 1234 by 1: " + lct.generalizeString("1234", 1));
//            System.out.println("Generalization String Test 1234 by 2: " + lct.generalizeString("1234", 2));
//            System.out.println("Generalization String Test 1234 by 3: " + lct.generalizeString("1234", 3));
//            System.out.println("Generalization String Test 1234 by 4: " + lct.generalizeString("1234", 4));
//
//            System.out.println("Generalization Price Test 9 by 0: " + lct.generalizePrice(9, 0));
//            System.out.println("Generalization Price Test 100 by 0: " + lct.generalizePrice(100, 0));
//            System.out.println("Generalization Price Test 50 by 1: " + lct.generalizePrice(50, 1));
//            System.out.println("Generalization Price Test 11 by 1: " + lct.generalizePrice(11, 1));
//            System.out.println("Generalization Price Test 7 by 2: " + lct.generalizePrice(7, 2));
//            System.out.println("Generalization Price Test 75 by 2: " + lct.generalizePrice(75, 2));
//            System.out.println("Generalization Price Test 150 by 2: " + lct.generalizePrice(150, 2));
//            System.out.println("Generalization Price Test 35 by 3: " + lct.generalizePrice(35, 3));
//            System.out.println("Generalization Price Test 1150 by 3: " + lct.generalizePrice(1150, 3));
//            System.out.println("Generalization Price Test 9999 by 4: " + lct.generalizePrice(9999, 4));
//            System.out.println("Generalization Price Test 120 by 4: " + lct.generalizePrice(120, 4));
//            System.out.println("Generalization Price Test 1073 by 4: " + lct.generalizePrice(1073, 4));
//            System.out.println("Generalization Price Test 300 by 5: " + lct.generalizePrice(300, 5));
//
//            System.out.println("Generalization DeptID Test 13 by 0: " + lct.generalizeDeptID(13, 0));
//            System.out.println("Generalization DeptID Test 13 by 1: " + lct.generalizeDeptID(13, 1));
//            System.out.println("Generalization DeptID Test 13 by 2: " + lct.generalizeDeptID(13, 2));
//            System.out.println("Generalization DeptID Test 13 by 3: " + lct.generalizeDeptID(13, 3));
//            System.out.println("Generalization DeptID Test 15 by 4: " + lct.generalizeDeptID(15, 4));
//            System.out.println("Generalization DeptID Test 15 by 5: " + lct.generalizeDeptID(15, 5));
//            System.out.println("Generalization DeptID Test 40 by 5: " + lct.generalizeDeptID(40, 5));
//            System.out.println("Generalization DeptID Test 40 by 6: " + lct.generalizeDeptID(40, 6));
//
//            System.out.println("Generalization Weight Test 5 by 0: " + lct.generalizeWeight(5, 0));
//            System.out.println("Generalization Weight Test 5 by 1: " + lct.generalizeWeight(5, 1));
//            System.out.println("Generalization Weight Test 5 by 2: " + lct.generalizeWeight(5, 2));
//            System.out.println("Generalization Weight Test 0 by 1: " + lct.generalizeWeight(0, 1));
//            System.out.println("Generalization Weight Test 9 by 2: " + lct.generalizeWeight(9, 2));
//            System.out.println("Generalization Weight Test 8 by 1: " + lct.generalizeWeight(8, 1));
//            System.out.println("Generalization Weight Test 6 by 2: " + lct.generalizeWeight(6, 2));
//            System.out.println("Generalization Weight Test 7 by 2: " + lct.generalizeWeight(7, 2));
//
//            System.out.println("Generalization Years Test 1996 by 0: " + lct.generalizeYears("1996", 0));
//            System.out.println("Generalization Years Test 1996 by 1: " + lct.generalizeYears("1996", 1));
//            System.out.println("Generalization Years Test 1996 by 2: " + lct.generalizeYears("1996", 2));
//            System.out.println("Generalization Years Test 1996 by 0: " + lct.generalizeYears("1985", 0));
//            System.out.println("Generalization Years Test 1996 by 1: " + lct.generalizeYears("1985", 1));
//            System.out.println("Generalization Years Test 1996 by 2: " + lct.generalizeYears("1985", 2));
//            System.out.println("Generalization Years Test 1996 by 0: " + lct.generalizeYears("2001", 0));
//            System.out.println("Generalization Years Test 1996 by 1: " + lct.generalizeYears("2001", 1));
//            System.out.println("Generalization Years Test 1996 by 2: " + lct.generalizeYears("2001", 2));

            lct.copyTable(lct.CPY_STUDENT_TBL, lct.STUDENT_TBL);

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

            lct.createDVTable();

            con.close();
        }
        catch (SQLException e)
        {
            System.out.println(e);
        }
    }
}
