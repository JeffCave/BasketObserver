//////////////////////////////////////
//	class ICOM - a utility class for Internet communication.
//	Communicate with an standard URL with postargs or searchargs,
//	and retrieve answers from it.
//
//	Usage:
//
//		String urlString = "http://www.yahoo.com/find.cgi";
//		ICom testICom  = new ICom( urlString , "search1=1&search2=2", "post1=1&post2=2" );
//		String results = testICom.execute();
//
//	remarks:
//		JR 10/10/1998:
//			- no proper handling of "no respons".

import java.io.*;
import java.net.*;

class ICom implements Runnable {

	////////////////////////////////////// class variables
	boolean	debug 	= false;			// if true debug output is printed to System.out

	////////////////////////////////////// data variables
	URL		url;						// Internet address of Server
	String	searchArgs;					// searchargs to send to the server
	String	postArgs;					// postargs to send to the server
	String	feedback;					// output of CGI after processing CGIcommand
	String	status;						// "not send","no response", "response received"
    public boolean isReady;                    // was all data send?
    Thread thread;
    
	////////////////////////////////////// Constructor
	//	address: url string, search: all searchArgs in one String seperated by ampersands,
	//	post: all postArgs in one String seperated by ampersands
	ICom(String address, String search, String post){

		if(debug){ new debug( "new ICom()" ); }

		searchArgs 	= search;
		postArgs 	= post;
		feedback	= new String();
       
		// append searchargs to end of URL if any
		if( !search.equals("") ){
			address = address + "?" + search;
		}
		try {
			url 	= new URL(address);
		} catch ( MalformedURLException e) {}

		status	= "not send";
	    isReady = false;
	}

	////////////////////////////////////// Methods
	// execute() : Send request to "url", return feedback.
	public void execute(){
	   thread = new Thread(this);
   	   thread.start();
	}   
    public void run() {	    
        isReady = false;

		if(debug){
			new debug( "ICom.execute()" );
			new debug( "\turl   : " + url );
			new debug( "\tpost  : " + postArgs );
			new debug( "\tsearch: " + searchArgs );
		}

		URLConnection connection=null;

		// open connection, send postargs if any
		try {
			connection= url.openConnection();
			connection.setUseCaches(false);

			// print postArgs to connection (if any)
			if( !postArgs.equals("") ){
				connection.setDoOutput(true);
				PrintStream out = new PrintStream(connection.getOutputStream());
				out.print( postArgs );
				out.close();
			}
		} catch( IOException e) {}; // standard Exception

        try { Thread.sleep(1000); } catch (InterruptedException e) {};

		try{
			// get feedback from server
			DataInputStream in = null;
			in = new DataInputStream(connection.getInputStream());
			String line = "";
			feedback = "";
			while( (line = in.readLine() ) != null){
				feedback = feedback + line + ",eol,";
			    try { Thread.sleep(10); } catch (InterruptedException e) {};
			}
			in.close();
		}
		catch( UnknownError e) {} // Broken Pipe at end of input
		catch( IOException e) {}; // standard Exception
        isReady = true;
	}

	// getFeedback() : get last "feedback" of "url".
	public String getFeedback(){
		return feedback;
	}

}
