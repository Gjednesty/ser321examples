package Assignment3Starter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.net.*;
import java.io.*;
import java.util.Map;

import java.nio.file.Paths;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.json.JSONException;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

/**
 * The ClientGui class is a GUI frontend that displays an image grid, an input
 * text box, a button, and a text area for status.
 * 
 * Methods of Interest ---------------------- show(boolean modal) - Shows the
 * GUI frame with the current state -> modal means that it opens the GUI and
 * suspends background processes. Processing still happens in the GUI. If it is
 * desired to continue processing in the background, set modal to false.
 * newGame(int dimension) - Start a new game with a grid of dimension x
 * dimension size insertImage(String filename, int row, int col) - Inserts an
 * image into the grid appendOutput(String message) - Appends text to the output
 * panel submitClicked() - Button handler for the submit button in the output
 * panel
 * 
 * Notes ----------- > Does not show when created. show() must be called to show
 * he GUI.
 * 
 */
public class ClientGui implements Assignment3Starter.OutputPanel.EventHandlers {
	public static String test;
	public static byte[] b;
	public static int row;
	public static int col;
	public static boolean gameStatus;
	public static int dim;

	public static OutputStream out = null;
	public static ObjectOutputStream os = null;
	public static ObjectInputStream in = null;

	JDialog frame;
	public static Socket serverSock = null;
	PicturePanel picturePanel;
	OutputPanel outputPanel;

	/**
	 * Construct dialog
	 */
	public ClientGui() {
		frame = new JDialog();
		frame.setLayout(new GridBagLayout());
		frame.setMinimumSize(new Dimension(500, 500));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// setup the top picture frame
		picturePanel = new PicturePanel();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.25;
		frame.add(picturePanel, c);

		// setup the input, button, and output area
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.75;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		outputPanel = new OutputPanel();
		outputPanel.addEventHandlers(this);
		frame.add(outputPanel, c);
	}

	/**
	 * Shows the current state in the GUI
	 * 
	 * @param makeModal - true to make a modal window, false disables modal behavior
	 */
	public void show(boolean makeModal) {
		frame.pack();
		frame.setModal(makeModal);
		frame.setVisible(true);
	}

	/**
	 * Creates a new game and set the size of the grid
	 * 
	 * @param dimension - the size of the grid will be dimension x dimension
	 */
	public void newGame(int dimension) throws IOException {
		try {
			picturePanel.newGame(dimension);
			outputPanel.appendOutput(test);
			picturePanel.insertImage(b, row, col);
			// outputPanel.appendOutput("Started new game with a " + dimension + "x" +
			// dimension + " board.");
			// main.insertImage("img/Pineapple-Upside-down-cake_1_1.jpg", 1, 1);
			// setupPic();
		} catch (PicturePanel.InvalidCoordinateException e) {
			e.getStackTrace();
			// put error in output
			
		}
	}

	// public void setupPic() throws IOException {
	// picturePanel.newGame(dim);
	// grid = setupGrid(dim);
	// insertImage(pngFiles.get(current), 0, 0);
	// show(true);
//	}

	/**
	 * Submit button handling
	 * 
	 * Change this to whatever you need
	 */
	@Override
	public void submitClicked() {
		// Pulls the input box text
		try {
			String input = outputPanel.getInputText();
			if (input.length() > 0) {
				outputPanel.appendOutput(input);
				// clear input text box
				outputPanel.setInputText("");
				String answer = "{'header': {'response': 'json'},'payload': {'answer': '" + input + "'}}";
				// write to the server
				System.out.println(answer);
				// out = serverSock.getOutputStream();
				// create an object output writer (Java only)
				// os = new ObjectOutputStream(out);
				// write the whole message
				os.writeObject(answer);
				// make sure it wrote and doesn't get cached in a buffer
				os.flush();

				// ObjectInputStream in = new ObjectInputStream(serverSock.getInputStream());
				String i = (String) in.readObject();
				outputPanel.appendOutput(i);

			}
		} catch (Exception e) {
			e.getStackTrace();
		}
	}

	/**
	 * Key listener for the input text box
	 * 
	 * Change the behavior to whatever you need
	 */
	@Override
	public void inputUpdated(String input) {
	}

	private static Response getResponse(Map header) throws RuntimeException {
		String response = (String) header.get("response");
		if (response.equals("json")) {
			return Response.JSON;
		} else {
			throw new java.lang.RuntimeException("Response type not found!");
		}
	}

	private static String getNum(Map payload, String key) {
		return (String) payload.get(key);
	}

	public static void main(String[] args) throws IOException {
		Socket serverSock = null;

		// PrintWriter out = null;
		// BufferedReader in = null;

		int port = 9000; // default port

		if (args.length != 2) {
			System.out.println("Expected arguments: <host(String)> <port(int)>");
			System.exit(1);
		}
		String host = args[0];
		try {
			port = Integer.parseInt(args[1]);
		} catch (NumberFormatException nfe) {
			System.out.println("[Port] must be integer");
			System.exit(2);
		}
		// read JSON data from the file

		try {
			// connect to the server
			serverSock = new Socket(host, port);
			ClientGui main = new ClientGui();

			while (!gameStatus) {
				try {
					System.out.println("Enter the dimmensions for the grid between 2-4:");
					Scanner scanner = new Scanner(System.in);
					dim = scanner.nextInt();
					if ((dim < 2) || (dim > 4))
						throw new IndexOutOfBoundsException();
					gameStatus = true;
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Dimmension must be between 2-4");
				} catch (Exception e) {
					System.out.println("Must be a number.");
				}
			}

			String setup = "{'header': {'response': 'json'},'payload': {'dim': '" + dim + "'}}";
			// write to the server
			out = serverSock.getOutputStream();
			// create an object output writer (Java only)
			os = new ObjectOutputStream(out);
			// write the whole message
			os.writeObject(setup);
			// make sure it wrote and doesn't get cached in a buffer
			os.flush();

			in = new ObjectInputStream(serverSock.getInputStream());
			String i = (String) in.readObject();
			System.out.println(i);
			String jsonData = (String) in.readObject();
			System.out.println("Received the String " + jsonData);
			// convert json string to a JSON object

			JSONTokener jsonTokener = new JSONTokener(jsonData);
			JSONObject data = new JSONObject(jsonTokener);

			// get the 'header' and the 'payload'
			JSONObject headerJSON = (JSONObject) data.get("header");
			JSONObject payloadJSON = (JSONObject) data.get("payload");

			Map header = headerJSON.toMap();
			Map payload = payloadJSON.toMap();

			Response response = getResponse(header);
			// int baseN = getBase(header, "base");

			test = getNum(payload, "message");
			b = getNum(payload, "img").getBytes();
			row = Integer.parseInt(getNum(payload, "row"));
			col = Integer.parseInt(getNum(payload, "col"));

			// read from the server
			// in = new ObjectInputStream(serverSock.getInputStream());
			// String i = (String) in.readObject();
			// System.out.println(i);
			main.newGame(dim);
			main.show(true);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (serverSock != null)
				serverSock.close();
		}

	}

	enum Response {
		JSON
	}

}
