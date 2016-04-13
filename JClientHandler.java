/*
 * JeopardyAssignment Submission 3 Question and Answer
 * JClientHandler.java
 *
 * handles communication between the client(jeopardy contestants)
 * and the server. 
 */
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;

public class JClientHandler implements Runnable
{
	private volatile static int round=1;
	private Socket connectionSock = null;
	private ArrayList<Socket> socketList;
	private volatile static ArrayList<Integer> scores;
	private volatile static ArrayList<String> playerNames;
	public volatile static int clientNum = 1;
	private int individualClientNum;
	public  int state = 0;
	public volatile static boolean firstBuzzClientAnswered = false;
	private volatile static ArrayList<Socket> buzzIn;
	private volatile static int clientToAnswer =0;
	private volatile static boolean wasCorrect = false;
	String[][] questionsArray = new String[5][];
	private int currentQuestion = 0;
	private volatile static String one = "";
	private volatile static String two = "";
	private volatile static String three = "";
	JClientHandler(Socket sock, ArrayList<Socket> socketList)
	{
		scores = new ArrayList<Integer>();
		playerNames = new ArrayList<String>();
		scores.add(0);scores.add(0);scores.add(0);
		this.connectionSock = sock;
		this.socketList = socketList;	// Keep reference to master list
		buzzIn = new ArrayList<Socket>();
		try{
			Scanner fileScanner = new Scanner(new File("JeopardyQuestions.txt"));
			int lineCount = 0;
			String line = "";
			while (fileScanner.hasNextLine()){
				line = fileScanner.nextLine();				
			   questionsArray[lineCount] = line.split(",");
			   lineCount++;
			}
			
		}
		catch(FileNotFoundException e){

		}
	}

	public void run()
	{
        // Get data from a client and send it to everyone else
		try
		{
			System.out.println("Connection made with socket " + connectionSock);
			BufferedReader clientInput = new BufferedReader(
				new InputStreamReader(connectionSock.getInputStream()));
			
			DataOutputStream clientOutput = new DataOutputStream(connectionSock.getOutputStream());
			boolean connected = false;
			
			String name = "";
			String buzzInStr = "";
			while (currentQuestion != 5)
			{
				round = currentQuestion + 1;
				switch(state){
					case 0:
						if (!connected) {
							name = clientInput.readLine();
							playerNames.add(name);
							System.out.println("name: " + name);
							clientOutput.writeBytes("You are contestant number: " + clientNum + "\n");
							individualClientNum = clientNum;
							clientNum++;
							connected = true;
							switch(individualClientNum){
								case 1:
									one = name;
									break;
								case 2:
									two = name;
									break;
								case 3:
									three = name;
									break;
							}
						}
						if (clientNum == 4) {
							state = 1;
						}
						break;
					case 1:
						// Turn around and output this data
						// to all other clients except the one
						// that sent us this information
						clientOutput = new DataOutputStream(connectionSock.getOutputStream());
						clientOutput.writeBytes("QUESTION: " + questionsArray[currentQuestion][0] + "\n");
						clientOutput.writeBytes("Enter 'b' to buzz in.\n");	
						
						wasCorrect = false;						
						state = 2;
						break;
					case 2:
						buzzInStr = clientInput.readLine();
						if (buzzInStr.equals("b")) {
							buzzIn.add(connectionSock);
							System.out.println(name + " buzzed in");
							state =3;
							buzzInStr = "";
						}
						break;
					case 3:
						if (buzzIn.get(clientToAnswer) == this.connectionSock) {
							
							clientOutput = new DataOutputStream(buzzIn.get(clientToAnswer).getOutputStream());
							clientOutput.writeBytes("You were the first to buzz in" + "\n");
							clientOutput.writeBytes("Submit your answer" + "\n");
							if (clientToAnswer > 0) {
								for (Socket s : socketList)
								{
									if (s != connectionSock)
									{
										clientOutput = new DataOutputStream(s.getOutputStream());
										clientOutput.writeBytes(name + " was the first to buzz in!" + "\n");
									}
								}
							}else{
								for (Socket s : socketList)
								{
									if (s != connectionSock)
									{
										clientOutput = new DataOutputStream(s.getOutputStream());
										clientOutput.writeBytes(name + " was the first to buzz in!" + "\n");
										clientOutput.writeBytes("Please still buzz in for a second chance" + "\n");
									}
								}
							}
							
							state = 4;
							
						}else{
							state = 5;
						}
						break;
					case 4:
						String question = clientInput.readLine();
						if (question != null)
						{
							System.out.println(name + ": " + question);
							// Turn around and output this data
							// to all other clients except the one
							// that sent us this information
							for (Socket s : buzzIn)
							{
								if (s != connectionSock)
								{
									clientOutput = new DataOutputStream(s.getOutputStream());
									clientOutput.writeBytes(name + " answered: " + question + "\n");
								}
							}
						}
						if (question.equalsIgnoreCase(questionsArray[currentQuestion][1])) {
							System.out.println("That is correct\n");
							for (Socket s : socketList)
							{
								clientOutput = new DataOutputStream(s.getOutputStream());
								clientOutput.writeBytes("That is correct\n");
							}
							int current = scores.get(individualClientNum-1);
							scores.set(individualClientNum-1,current+10);

							System.out.println("\n******** " + "ROUND "+ round +" SCORES ********\n");
							System.out.println(one + ": " + scores.get(0));
							System.out.println(two + ": " + scores.get(1));
							System.out.println(three + ": " + scores.get(2) + "\n\n");
							for(Socket s: socketList){
								clientOutput = new DataOutputStream(s.getOutputStream());
								clientOutput.writeBytes("\n******** " + "ROUND "+ round +" SCORES ********\n");
								clientOutput.writeBytes(one + ": " + scores.get(0) + "\n");
								clientOutput.writeBytes(two + ": " + scores.get(1) + "\n");
								clientOutput.writeBytes(three + ": " + scores.get(2) + "\n\n");
							}
							state = 5;
							
							wasCorrect = true;
						}
						else{
							System.out.println("\nThat is incorrect");
							int current = scores.get(individualClientNum-1);
							scores.set(individualClientNum-1,current-10);
							for (Socket s : buzzIn)
							{
								clientOutput = new DataOutputStream(s.getOutputStream());
								clientOutput.writeBytes("\nThat is incorrect" + "\n");
								
							}
							clientToAnswer++;
							firstBuzzClientAnswered = true;
							wasCorrect =false;
							state = 5;
							if (clientToAnswer == 3) {
								clientOutput = new DataOutputStream(connectionSock.getOutputStream());
								clientOutput.writeBytes("\nThe correct answer is: " + questionsArray[currentQuestion][1] + '\n');
								state = 1;
								currentQuestion++;
								
								System.out.println("\n******** " + "ROUND " + round +" SCORES ********" + "\n");
								System.out.println(one + ": " + scores.get(0));
								System.out.println(two + ": " + scores.get(1));
								System.out.println(three + ": " + scores.get(2) + "\n\n");
								for(Socket s: socketList){
									clientOutput = new DataOutputStream(s.getOutputStream());
									clientOutput.writeBytes("\n******** " + "ROUND "+ round +" SCORES ********" + "\n");
									clientOutput.writeBytes(one + ": " + scores.get(0) + "\n");
									clientOutput.writeBytes(two + ": " + scores.get(1) + "\n");
									clientOutput.writeBytes(three + ": " + scores.get(2) + "\n\n");
								}
									

							}
						}
						
						break;
					case 5:
						//clientOutput.writeBytes("state 5");

						if (wasCorrect) {
							state = 1;
							buzzIn.clear();
							clientToAnswer = 0;
							currentQuestion++;
						}else{
							if (firstBuzzClientAnswered) {
								if (clientToAnswer == 3) {
									clientOutput = new DataOutputStream(connectionSock.getOutputStream());
									clientOutput.writeBytes("The correct answer is: " + questionsArray[currentQuestion][1] + "\n\n");
									currentQuestion++;
									buzzIn.clear();
									state = 1;
									clientToAnswer = 0;
									
								}else{
									state = 3;
								
									firstBuzzClientAnswered = false;
								}
								
							}
						}
						

						break;
				}
				// Get data sent from a client
				/*
				else
				{
				  // Connection was lost
				  System.out.println("Closing connection for socket " + connectionSock);
				   // Remove from arraylist
				   socketList.remove(connectionSock);
				   connectionSock.close();
				   break;
				}
				*/
			}if (individualClientNum == 1) {
				System.out.println("\n******** FINAL SCORES ********" + "\n");
				System.out.println(one + ": " + scores.get(0));
				System.out.println(two + ": " + scores.get(1));
				System.out.println(three + ": " + scores.get(2) + "\n");
				for(Socket s: socketList){
					clientOutput = new DataOutputStream(s.getOutputStream());
					clientOutput.writeBytes("\n******** FINAL SCORES ********" + "\n");
					clientOutput.writeBytes(one + ": " + scores.get(0) + "\n");
					clientOutput.writeBytes(two + ": " + scores.get(1) + "\n");
					clientOutput.writeBytes(three + ": " + scores.get(2) + "\n");
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
