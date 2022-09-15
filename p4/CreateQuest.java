import java.util.*;
import java.net.*;
import java.text.*;
import java.lang.*;
import java.io.*;
import java.sql.*;
import java.util.Date;
import pgpass.*;
/*============================================================================
CLASS CreateQuest
============================================================================*/

public class CreateQuest {
    private Connection conDB;   // Connection to the database system.
    private String url;         // URL: Which database?

    private String  user = "jiahao18";
    private String  passwd;
    
    private List<String> list;
    private java.sql.Date d;
    private String day;
    private String realm;
    private String theme;
    private String amount;

    // Constructor
    public CreateQuest (String[] args) {
        // Set up the DB connection.
        try {
            // Register the driver with DriverManager.
            Class.forName("org.postgresql.Driver").newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (InstantiationException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(0);
        }
        
        if (args.length < 4) {
            // Don't know what's wanted.  Bail.
            System.out.println("\nUsage: java CreateQuest input #parameters is wrong\n");
            System.exit(0);
        }
        else {
        	day 	= new String(args[0]);
        	
        	realm 	= new String(args[1]);
        	theme 	= new String(args[2]);
        	amount 	= new String(args[3]);
        }
        
        if (args.length >= 5){
           user = args[4];
        }
        list = new LinkedList<String>();
        // Properties props = new Properties();
        try {
            passwd = PgPass.get("db", "*", user, user);
            // props.setProperty("user",    user);
            // props.setProperty("password", passwd);
            // props.setProperty("ssl","true"); // NOT SUPPORTED on DB
        } catch(Exception e) {
            System.out.print("\nCould not obtain PASSWD from <.pgpass>.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
        // URL: Which database?
        url = "jdbc:postgresql://db:5432/";

        // Initialize the connection.
        try {
            // Connect with a fall-thru id & password
            conDB = DriverManager.getConnection(url, user,passwd);
        } catch(SQLException e) {
            System.out.print("\nSQL: database connection error.\n");
            System.out.println(e.toString());
            System.exit(0);
        }    

        // Let's have autocommit turned off.  No particular reason here.
        try {
            conDB.setAutoCommit(false);
        } catch(SQLException e) {
            System.out.print("\nFailed trying to turn autocommit off.\n");
            e.printStackTrace();
            System.exit(0);
        }    

        if (!dateCheck(day)) {
            System.out.print("Error#1: day is not in future: ");
            System.out.print("date \"" + args[0] +"\" is not a day in future. Bye\n");
            System.exit(0);
        }
        if (!realmCheck(realm)) {
            System.out.print("Error#2: realm does not exist: ");
            System.out.print("realm \"" + args[1] +"\" does not exist in Realm. Bye\n");
            System.exit(0);
        }
        if (args.length >= 6) {
        	if (!seedCheck(args[5])) {
        		System.out.print("Error#4: seed value is improper: ");
                System.out.print("seed value " + args[5] +" is not between -1 and 1. Bye\n");
                System.exit(0);
        	}
        	else
        	{
                setseed(args[5]);
        	}
         }
        
        if (!amountCheck(amount)) {
            System.out.print("Error#3: amount exceeds what is possible: ");
            System.out.print("amount " + args[3] +" can't be reached by loot rules. Bye\n");
            System.exit(0);
        }
        // Report total sales for this customer.
        insertQuest();
        // Commit.  Okay, here nothing to commit really, but why not...
        try {
            conDB.commit();
        } catch(SQLException e) {
            System.out.print("\nFailed trying to commit.\n");
            e.printStackTrace();
            System.exit(0);
        }    
        // Close the connection.
        try {
            conDB.close();
        } catch(SQLException e) {
            System.out.print("\nFailed trying to close the connection.\n");
            e.printStackTrace();
            System.exit(0);
        }    

    }

	public boolean amountCheck(String s) {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.
        boolean           result    = false;  // Return.
        int				  value 	= 0;
        int				  a	= Integer.parseInt(s);
        queryText =
            "select *       " + 
            "from Treasure  " + 
            "order by random();";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("amountcheck failed in prepare\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("amountcheck failed in execute\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            while (answers.next() && value < a) {
            	list.add(answers.getString("treasure"));
            	value += answers.getInt("sql");
            } 
            if (value >= a) {
            	result = true;
            }
        } catch(SQLException e) {
            System.out.println("amountcheck failed in cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("amountcheck failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
        
        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("amountcheck failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        return result;
    }
    
    public boolean seedCheck(String s) {
    	float f = Float.parseFloat(s);
        if (f <= 1 && f >= -1) {
        	return true;
        }
        return false;
    }


	public boolean realmCheck(String s) {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.

        boolean           result    = false;  // Return.

        queryText =
            "SELECT realm       "
          + "FROM Realm "
          + "WHERE realm = ?    ";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("realmCheck failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setString(1, s);
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("realmCheck failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            if (answers.next()) {
            	result = true;
            } else {
            	result = false;
            }
        } catch(SQLException e) {
            System.out.println("realmCheck failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("realmCheck failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("realmCheck failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        return result;
    }

	public void setseed(String s) {
    	String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.
        float             seed      = Float.parseFloat(s);
        queryText =
            "select setseed( ? );";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("setseed failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setFloat(1, seed);
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("setseed failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("setseed failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("setseed failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
    }
    
	public boolean dateCheck(String s) {
        Date date = new Date();
        boolean           result      = false;  // Return.
        SimpleDateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd");
        try {
			Date in = formatter.parse(s);
			d = new java.sql.Date(in.getTime());
			if (date.compareTo(in) < 0) {
				result = true;
			}
		} catch (ParseException e) {
			System.out.print("DateCheck failed parsing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
		}
        
        return result;
    }

    public void insertQuest() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
           // A cursor.

        queryText =
            "insert into Quest (theme, realm, day, succeeded) values " + 
            " ( ? , ? , ? , NULL );       " +
        	"insert into Loot (loot_id, treasure, theme, realm, day, login) values ";
        for (int i  = list.size(),j = 0; j < i ; j++) {
        	queryText += " ( ? , ? , ? , ? , ? , NULL )";
        	if (j == i -1) {
        		queryText += "; ";
        	}
        	else {
        		queryText += ", ";
        	}
        }
        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("insertQuest failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
        	querySt.setString(1, theme);
        	querySt.setString(2, realm);
        	querySt.setDate(3, d);
        	int c = 4;
        	for (int i  = list.size(),j = 0; j < i ; j++) {
        		querySt.setInt(c++, j + 1);
            	querySt.setString(c++, list.get(j));
            	querySt.setString(c++, theme);
            	querySt.setString(c++, realm);
            	querySt.setDate(c++, d);
        	}
            int num = querySt.executeUpdate();
        } catch(SQLException e) {
            System.out.println("insertQuest failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("insertQuest failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

    }

    public static void main(String[] args) {
        CreateQuest ct = new CreateQuest(args);
    }
 }