package examinee.model;

import java.awt.AWTException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.Timer;

import common.network.*;
import javafx.beans.property.IntegerProperty;

/**
 * Model class for examinee
 * @author Alper Sari
 * @version 09/05/2019
 */
public class Model
{
	// properties

	private static final String SECRET = "24DdwVJljT28m6MSOfvMnj7iZbL8bNMmo7xnLKsZSyurflOLg2JFtq0hsY09";
	public final int STATUS_CONNECTED = 1;
	public final int STATUS_DISCONNECTED = 2;
	public final int STATUS_LOGIN = 3;
	public final int STATUS_FINISHED = 4;

	private static Model instance;

	private String username;
	private String adminIP;
	private boolean dataEnd;
	private int timeRemain;
	private int timeTotal;
	private IntegerProperty status;
	private Client client;
	private ExamEntry reference;
	private ExamEntry examData;
	private Timer timer;

	// constructors

	private Model()
	{
		status.set(0);
		reference = null;
		dataEnd = false;
		
		timer = new Timer( 1000, new TimerListener());
		timer.stop();
	}

	/**
	 * Get singleton object
	 * @return Instance of Model
	 */
	public static synchronized Model getInstance()
	{
		if ( instance == null)
			instance = new Model();
		return instance;
	}

	/**
	 * Send message to server using client instance
	 * @param protocol - Protocol when sending message
	 * @param msg - Message to send
	 */
	public void sendMessage( String protocol, String msg)
	{
		client.sendMessage( SECRET + ":::" + protocol + ":::" + msg);
	}
	
	
	
	/**
	 * Create client with network package and connect to admin ip
	 * @param name Name of user
	 * @param ip Admin ip to connect to
	 * @return true if connection successful
	 */
	public boolean login( String name, String ip)
	{
		adminIP = ip;
		username = name;
		try
		{
			client = new Client( adminIP) {

				@Override
				public void messageReceived( String msg)
				{
					handleMessage( msg); // Pass message to handle method
				}

			};

			sendMessage( "name", username);
			status.set(STATUS_CONNECTED);;
			
			return true;
		}
		catch (IOException e )
		{
			//Login failed
			return false;
		}

	}
	
	/**
	 * Submits all question part entries
	 */
	public void submitAll()
	{
		ArrayList<ExamEntry> list = examData.getAll();
		for( int i = 0; i < list.size(); i++)
		{
			if ( list.get(i) instanceof Question )
			{
				QuestionPart part = null;
				ArrayList<ExamEntry> partList = list.get(i).getAll();
				for( int k = 0; k < partList.size(); k++)
				{
					if ( partList.get(k) instanceof QuestionPart)
					{
						part = (QuestionPart) partList.get(k);
						part.submit( client);
					}
				}
			}
		}
		status.set(STATUS_FINISHED);
	}
	
	/**
	 * Get total exam time
	 * @return Total time
	 */
	public int getTimeTotal()
	{
		return timeTotal;
	}
	
	/**
	 * Get remaining exam time
	 * @return Remaining time
	 */
	public int getTimeRemain()
	{
		return timeRemain;
	}
	
	/**
	 * Get status as integer property
	 * @return status
	 */
	public IntegerProperty getStatus()
	{
		return status;
	}
	
	/**
	 * Get ExamEntry instance containing all other entries
	 * @return examData
	 */
	public ExamEntry getExamData()
	{
		return examData;
	}
	
	/**
	 * Returns true if examData has been completely filled
	 * @return dataEnd
	 */
	public boolean isExamReady()
	{
		return dataEnd;
	}
	
	/**
	 * Searches the existing exam data for a given id
	 * @param id-Id to search for
	 * @return Found entry
	 */
	private ExamEntry searchId( String id)
	{
		ArrayList<ExamEntry> list = examData.getAll();
		ExamEntry result = null;
		
		for( int i = 0; i < list.size(); i++)
		{
			if ( list.get(i).getId().equals(id) )
				result = list.get(i);
		}
		return result;
	}
	
	/**
	 * Handles message according to protocol
	 * @param msg Received message
	 */
	private void handleMessage( String msg)
	{
		String parts[] = msg.split( ":::");

		// Create an Instruction, goes into exam
		if ( parts[1].equals( "instruction"))
		{
			reference = new Instruction( parts[2], parts[3], parts[4], false, false);
			ExamEntry parent = searchId( parts[5]);
			parent.add(reference);
			reference.setParent( parent);
		}
		
		// Create a Question, goes into exam
		if ( parts[1].equals( "question") )
		{
			reference = new Question( parts[2], parts[3], parts[4], true, false);
			ExamEntry parent = searchId( parts[5]);
			parent.add(reference);
			reference.setParent( parent);
		}
		
		// Create part, goes into Question
		if ( parts[1].equals( "part") )
		{
			reference = new QuestionPart( parts[2], parts[3], parts[4], true, true);
			ExamEntry parent = searchId( parts[5]);
			parent.add(reference);
			reference.setParent( parent);
		}
		
		// Add template solution to question part
		if ( parts[1].equals( "template") )
		{
			QuestionPart part = (QuestionPart) searchId( parts[4]);
			part.updateSolution( parts[3]);
		}
		
		// Create an exam, everything else is placed within this container
		if ( parts[1].equals( "exam") && reference == null )
		{
			examData = new ExamEntry( parts[2], parts[3], "", false, false);
			timeTotal = Integer.parseInt(parts[4]); // Time in seconds
			timeRemain = timeTotal;
		}
		
		// 
		if ( parts[1].equals( "data_end") )
		{
			dataEnd = !dataEnd;
			timer.start();
		}
		
		if ( parts[1].equals( "exam_ended") )
		{
			submitAll();
			client.close();
		}
		
		// Send screenshot after server request
		if ( parts[1].equals( "screenshot_scale") )
		{
			try
			{
				Screenshot screen = new Screenshot( Integer.parseInt( parts[2]));
				client.sendImage( screen);
			} catch (NumberFormatException e)
			{
				e.printStackTrace();
			} catch (AWTException e)
			{
				e.printStackTrace();
			}
		}
		
		if ( parts[1].equals( "screenshot_width") )
		{
			try
			{
				Screenshot screen = new Screenshot( Integer.parseInt( parts[2]));
				client.sendImage( screen);
			} catch (NumberFormatException e)
			{
				e.printStackTrace();
			} catch (AWTException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private class TimerListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e)
		{
			timeRemain--;
		}
		
	}
	
	//******************************************************
	// Test
	//******************************************************
//	examData = new ExamEntry( "123", "test", "", false, false);
//	timeTotal = 6000; // Time in seconds
//	timeRemain = timeTotal;
//			
//	reference = new Instruction( "001", "Instruction", "Don't Cheat", false, false);
//	examData.add( reference);
//	reference.setParent( examData);
//			
//	reference = new Question( "002", "Question 1", "Why are we here?", true, false);
//	examData.add( reference);
//	reference.setParent(examData);
//					
//	reference.add( new QuestionPart( "101", "part 1", "test1", true, true));		
//	reference.add( new QuestionPart( "102", "part 2", "test2", true, true));		
//			
	//******************************************************
}

