/**
 * JClientHandler.java
 *
 * This class handles communication between the client
 * and the server.  It runs in a separate thread but has a
 * link to a common list of sockets to handle broadcast.
 *
 */
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;

public class JClientHandler implements Runnable
{
	private Socket connectionSock = null;
	private ArrayList<Socket> socketList;
	public static int clientNum = 1;

	JClientHandler(Socket sock, ArrayList<Socket> socketList)
	{
		this.connectionSock = sock;
		this.socketList = socketList;	// Keep reference to master list
	}

	public void run()
	{
        		// Get data from a client and send it to everyone else
		try
		{
			System.out.println("Connection made with socket " + connectionSock);
			BufferedReader clientInput = new BufferedReader(
				new InputStreamReader(connectionSock.getInputStream()));
			String name = clientInput.readLine();
			DataOutputStream clientOutput = new DataOutputStream(connectionSock.getOutputStream());
			
			// print client number
			clientOutput.writeBytes("Welcome, Client number: " + clientNum + "\n");
			clientOutput.writeBytes("Enter 1 to hit the buzzer!\n");

			clientNum++;

			while (true)
			{
				// Get data sent from a client
				String clientText = clientInput.readLine();
				if (clientText != null)
				{
					if(clientText.equals("1"))
						System.out.println("BUZZ! " + name + " has hit the buzzer.");
					else
					{
					System.out.println(name + ": " + clientText);
					// Turn around and output this data
					// to all other clients except the one
					// that sent us this information
					}

					for (Socket s : socketList)
					{
						if (s != connectionSock)
						{
							clientOutput = new DataOutputStream(s.getOutputStream());
							clientOutput.writeBytes(name + ": " + clientText + "\n");
						}

					}
				}
				else
				{
				  // Connection was lost
				  System.out.println("Closing connection for socket " + connectionSock);
				   // Remove from arraylist
				   socketList.remove(connectionSock);
				   connectionSock.close();
				   break;
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error: " + e.toString());
			// Remove from arraylist
			socketList.remove(connectionSock);
		}
	}
} // JClientHandler for MTServer.java
