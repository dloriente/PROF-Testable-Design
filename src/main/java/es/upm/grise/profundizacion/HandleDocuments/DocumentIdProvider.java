package es.upm.grise.profundizacion.HandleDocuments;

import static es.upm.grise.profundizacion.HandleDocuments.Error.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DocumentIdProvider {

	// Environment variable
	private static String ENVIRON  = "APP_HOME";
	private static String path = System.getProperty("user.dir") + "/";  //https://stackoverflow.com/questions/4032957/how-to-get-the-real-path-of-java-application-at-runtime
	private static String driverDB = "com.mysql.cj.jdbc.Driver";

	// ID for the newly created documents
	protected int documentId;

	// Connection to database (open during program execution)
	Connection connection = null;

	// Singleton access
	private static DocumentIdProvider instance;

	public static DocumentIdProvider getInstance() throws NonRecoverableError {
		if (instance != null)

			return instance;

		else {

			instance = new DocumentIdProvider(path, driverDB);
			return instance;

		}
	}

	public DocumentIdProvider(int documentId) {
		this.documentId = documentId;
	}

	// Create the connection to the database
	public DocumentIdProvider(String path, String driverDB) throws NonRecoverableError {

		// If ENVIRON does not exist, null is returned
//		String path = System.getenv(ENVIRON);
// 		path se define ahora como argumento de entrada en el constructor DocumentIdProvider

		if (path == null) {

			System.out.println(UNDEFINED_ENVIRON.getMessage());
			throw new NonRecoverableError(UNDEFINED_ENVIRON.getMessage());

		} else {

			Properties propertiesInFile = new Properties();
			InputStream inputFile = null;

			// Load the property file
			try {
				inputFile = new FileInputStream(path + "config.properties");
				propertiesInFile.load(inputFile);

			} catch (FileNotFoundException e) {

				System.out.println(NON_EXISTING_FILE.getMessage());
				throw new NonRecoverableError(NON_EXISTING_FILE.getMessage());

			} catch (IOException e) {

				System.out.println(CANNOT_READ_FILE.getMessage());
				throw new NonRecoverableError(CANNOT_READ_FILE.getMessage());

			}

			// Get the DB username and password
			String url = propertiesInFile.getProperty("url");
			String username = propertiesInFile.getProperty("username");
			String password = propertiesInFile.getProperty("password");

			// Load DB driver
			try {

//				Class.forName("com.mysql.jdbc.Driver").newInstance();  //Deprecated class
//				Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
				Class.forName(driverDB).newInstance();

			} catch (InstantiationException e) {

				System.out.println(CANNOT_INSTANTIATE_DRIVER.getMessage());
				throw new NonRecoverableError(CANNOT_INSTANTIATE_DRIVER.getMessage());

			} catch (IllegalAccessException e) {

				System.out.println(CANNOT_INSTANTIATE_DRIVER.getMessage());
				throw new NonRecoverableError(CANNOT_INSTANTIATE_DRIVER.getMessage());

			} catch (ClassNotFoundException e) {

				System.out.println(CANNOT_FIND_DRIVER.getMessage());
				throw new NonRecoverableError(CANNOT_FIND_DRIVER.getMessage());

			}

			// Create DB connection
			try {

				connection = DriverManager.getConnection(url, username, password);

			} catch (SQLException e) {

				System.out.println(CANNOT_CONNECT_DATABASE.getMessage());
				throw new NonRecoverableError(CANNOT_CONNECT_DATABASE.getMessage());

			}

			// Read from the COUNTERS table
			String query = "SELECT documentId FROM Counters";
			Statement statement = null;
			ResultSet resultSet = null;

			try {

				statement = connection.createStatement();
				resultSet = statement.executeQuery(query);

			} catch (SQLException e) {

				System.out.println(CANNOT_RUN_QUERY.getMessage());
				throw new NonRecoverableError(CANNOT_RUN_QUERY.getMessage());

			}

			// Get the last objectID
			int numberOfValues = 0;
			try {

				while (resultSet.next()) {

					documentId = resultSet.getInt("documentId");
					numberOfValues++;

				}

			} catch (SQLException e) {

				System.out.println(INCORRECT_COUNTER.getMessage());
				throw new NonRecoverableError(INCORRECT_COUNTER.getMessage());

			}

			// Only one objectID can be retrieved
			if(numberOfValues != 1) {

				System.out.println(CORRUPTED_COUNTER.getMessage());
				throw new NonRecoverableError(CORRUPTED_COUNTER.getMessage());

			}

			// Close all DB connections
			try {

				resultSet.close();
				statement.close();

			} catch (SQLException e) {

				System.out.println(CONNECTION_LOST.getMessage());
				throw new NonRecoverableError(CONNECTION_LOST.getMessage());

			}
		}
	}

	// Return the next valid objectID
	public int getDocumentId() throws NonRecoverableError {

		documentId++;

		// Access the COUNTERS table
		String query = "UPDATE Counters SET documentId = ?";
		int numUpdatedRows;

		// Update the documentID counter
		try {

			PreparedStatement preparedStatement = connection.prepareStatement(query);
			preparedStatement.setInt(1, documentId);
			numUpdatedRows = preparedStatement.executeUpdate();

		} catch (SQLException e) {

			System.out.println(e.toString());
			System.out.println(CANNOT_UPDATE_COUNTER.getMessage());
			throw new NonRecoverableError(CANNOT_UPDATE_COUNTER.getMessage());

		}

		// Check that the update has been effectively completed
		if (numUpdatedRows != 1) {

			System.out.println(CORRUPTED_COUNTER.getMessage());
			throw new NonRecoverableError(CORRUPTED_COUNTER.getMessage());

		}

		return documentId;

	}
}
