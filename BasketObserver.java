    /*
    EggObserver
    */
    
    import java.applet.*;
    import java.awt.*;
    import java.io.*;
    import java.net.*;
    import java.util.*;
    
    public class BasketObserver extends Applet implements Runnable {
    
    // Basket Variables
    int samp_rec = 0;			// samples per record
    int sec_rec  = 0; 		// seconds per record
    int rec_pkt  = 0;			// record per packet
    int trialsz  = 0; 		// Trials per sample
    int nummEggs = 0; 		// Eggs reporting
    String startTime = ""; 	// Start Time
    String endTime   = ""; 	// End Time
    int tableSeconds = 0;	// table Seconds
    float EggDataArr[][] = new float[20][50];		// Data is stored in this array
    int nrOfEggs = 0;
    String RawBasketExtract = "";
    Thread animator = null;
    protected String[] Data;
    int dataindex=0;
    int ndata = 0;
    boolean growingBars=false;
    
    Date stimeDate = null;
    
    Color c1 = new Color(0xFF0000);
    Color c2 = new Color(0x00FF00);
    Color c3 = new Color(0x0000FF);
    Color c4 = new Color(0xF0000F);
    Color c5 = new Color(0x123456);
    Color c6 = new Color(0x654321);
    
    
    Font font = new Font("Comic Sans MS", Font.BOLD,24);
    Image offscreen;
    int imagewidth,imageheight;
    int stringwidth,stringheight,stringascent;
    
    int GlobalCounter=0;
    
    public void init(){
    	URL sound_url=null;
    	AudioClip soundtrack = null;
    	try {
    			sound_url = new URL( this.getDocumentBase(), "bgmusic.au");
    	}
    	catch (MalformedURLException e) {};
    	soundtrack = getAudioClip(sound_url);
    	soundtrack.loop();
    
     	//System.out.println("init ");
     	
     	// interface
    	Dimension size = this.size();
    	FontMetrics fm = this.getFontMetrics(font);
    	stringwidth = fm.stringWidth("hallo");
    	stringheight = fm.getHeight();
    	stringascent = fm.getAscent();
    	
    	//
    	Graphics g = this.getGraphics();
    	drawBackground( g );
    	
    	g.setColor(Color.white);
       	g.setFont(font);
       	g.drawString("Please Wait" , 250,80);
       
     	// calculus
     	nrOfEggs = 0;
     	for( int i=0; i<20; i++){
     		for( int  j=0; j<20; j++){
     			EggDataArr[i][j] = -1;
     		}
     	}
     	
     	// time
     	stimeDate = null;
     	GlobalCounter=0;
    }
    
    public void start(){
       animator = new Thread(this);
       animator.start();
       
       Graphics g = this.getGraphics();
    	drawBackground( g );
    }
    
    public void stop(){
       if(animator != null ) animator.stop();
       animator=null;
    }
    
    public void run(){
    
    	Rectangle newrect = new Rectangle();
      Rectangle oldrect = new Rectangle();
      Rectangle r = new Rectangle();
    
    
     	while(true){
     	
    	 	
    
     		fetch();
     		parse();
     		
     		// animate
     		Dimension d = this.size();
    		if(
    		 		(imagewidth != d.width) ||
    		 		(d.height != imageheight)
    		 	){
    		 	offscreen = this.createImage(d.width, d.height);
    		 	imagewidth = d.width;
    		 	imageheight = d.height;
    		}
    
    		
    		for( int i=0; i<20; i++){
    			
    			
    			oldrect.reshape(0, 0, imagewidth, imageheight);
    	 		newrect.reshape(0, 0, imagewidth, imageheight);
       		r = newrect.union(oldrect);
       		
    	 		Graphics g = offscreen.getGraphics();
       		g.clipRect(r.x,r.y,r.width,r.height);
       		
       		GlobalCounter=i+1;
       		
       		paint(g);
       		
       		g = null;
       		
       		
       		g = this.getGraphics();
       		g.clipRect(r.x,r.y,r.width,r.height);
       		g.drawImage(offscreen,0,0,this);
       		
       		g = null;
       		
       		try { animator.sleep(1000); } catch ( InterruptedException e ) {};
       	}
     		
     		//try { animator.sleep(1000); } catch ( InterruptedException e ) {};
     	}
    }
    
    public void parse(){
    	
    	// make array of raw data
    	StringTokenizer t = new StringTokenizer(RawBasketExtract, ",");
    	ndata = t.countTokens();
    	Data = new String[ndata];
    	for(int i=0; i<ndata; i++)
    		{ Data[i] = t.nextToken(); }
    	t = null;
    	
    	// find start of Eggnumbers (data before Eggnumbers are dismissed for now)
    	int i=0;
    	String tmp = new String("\"Date/Time\"");
    	while( ! (Data[i++].equals(tmp) ) ) {};
    	// save Eggnumbers
    	tmp = new String("eol");
    	nrOfEggs = 0;
    	while( ! (Data[i].equals(tmp) ) ) {
    		//System.out.println("Egg: " + Data[i]);
    		EggDataArr[nrOfEggs++][0] = Integer.parseInt(Data[i]);
    		i++;
    	}
    	// get Eggresults
    	i+=4;
    	int j=0;
    	int k=1;
    	while( i< Data.length ) {
    		while( ! (Data[i].equals(tmp) ) ){
    			EggDataArr[j++][k] = ZSquared( Integer.parseInt(Data[i]) );
    			i++;
    		}
    		i+=4; j=0; k++;
    	}
    	
    }
    
    public float ZSquared( int  v ){
        return (float) Math.pow(  (((float) v-100)/ Math.sqrt(50) ), 2 );
    }
    
    public void fetch(){
    	
    	URL url = null;
    	URLConnection connection=null;
    	DataInputStream in = null;
    	
    	//System.out.println("fetch entered");
    	
    	String line = null;
    	
    	Date d = new Date();
    	
    	
    	if( stimeDate == null ){
    		int offset = (1000 * 60 * (d.getTimezoneOffset() ) ) - (1000 * 60 * 60); // offset of 1 hour (getTimezoneOffset)
    		d.setTime( d.getTime() + offset );
    		stimeDate= new Date();
    		stimeDate.setTime(d.getTime());
    	}
    	else{
    		d.setTime(stimeDate.getTime());
    	}
    	
    	int Day = d.getDate();
    	int Month =  d.getMonth() + 1;
    	int Year = 1900 +  d.getYear();
    	int Hour = d.getHours();
    	int Minute = d.getMinutes();
    	int Second = d.getSeconds();
    	
    	//System.out.println(d.toString());
    	
    	String stime = Hour<10 ? "0" + Integer.toString(Hour) : Integer.toString(Hour);
    	stime = stime + "%3A" + ( Minute<10 ? "0" + Integer.toString(Minute) : Integer.toString(Minute) );
    	stime = stime + "%3A" + ( Second<10 ? "0" + Integer.toString(Second) : Integer.toString(Second) );
    	
    	d.setTime(d.getTime() + 1000*20);
    	Hour = d.getHours();
    	Minute = d.getMinutes();
    	Second = d.getSeconds();
    	//System.out.println(d.toString());
    	
    	String etime = Hour<10 ? "0" + Integer.toString(Hour) : Integer.toString(Hour);
    	etime = etime + "%3A" + ( Minute<10 ? "0" + Integer.toString(Minute) : Integer.toString(Minute) );
    	etime = etime + "%3A" + ( Second<10 ? "0" + Integer.toString(Second) : Integer.toString(Second) );
    	
    	//System.out.println(etime);
    	
    	String SearchArgs = "";
    	SearchArgs= SearchArgs.concat("?z=1&year=" + Integer.toString(Year) + "&month=" + Integer.toString(Month) + "&day=" + Integer.toString(Day));
    	SearchArgs= SearchArgs.concat("&stime=" + stime + "&etime=" +etime + "&idate=Yes");
    
    	//System.out.println(SearchArgs);
    	
    	//String BasketURL = "http://145.18.117.12/eggdatareq.pl";
    	//String BasketURL = "http://145.18.117.12/DATATEST.QRY";
    	String BasketURL = "http://noosphere.princeton.edu/cgi-bin/eggdatareq.pl" + SearchArgs;
    	
    	RawBasketExtract = "";
    	
    	try { url = new URL( BasketURL );}  catch( MalformedURLException e) {};
    	try { connection=  url.openConnection(); } catch( IOException e) {};
    	
    	try {
    		in = new DataInputStream(connection.getInputStream());
    		
    		while( (line = in.readLine() ) != null){
    			RawBasketExtract = RawBasketExtract + "" + line + ",eol,";
    			//System.out.println(line);
    		}
    		in.close();
    	}
    	catch( UnknownError e) {} // to prevend cancelations from Broken Pipe at end of file
    	catch( IOException e) {}; // standard Exception
    }
    
    //
    public boolean mouseDown(Event e, int x, int y){
       	if( growingBars) growingBars = false;
       	else { growingBars = true; }
       
       	return true;
      }
    
    // graphics
    
    
    void drawBackground(Graphics gr){
    
       Dimension size = this.size();
       gr.setColor( Color.black);
       gr.fillRect(0,0,size.width,size.height);
    
    }
    
    
    
    
    public void paint(Graphics g){
      	
      	drawBackground(g);
       
       for( int i=0; i< nrOfEggs; i++){
       	float x = EggDataArr[i][GlobalCounter];
       	
       	Color c = new Color ( 	(int) x < 9 ? 75 + (int) x*20 : 255,
       					(int) x < 16 ? 0 : ( (int) x < 25 ? 75 + (int) (x-16)*20 : 255 ),
       					0
       				  );
       
       	g.setColor(c);
       	
       	if( x > 4 & x < 9 ) {
       		play( getCodeBase(), "1.au");
       	}
       	if( x >= 9 & x < 14 ) {
       		play( getCodeBase(), "2.au");
       	}
       	if( x >= 14 ) {
       		play( getCodeBase(), "3.au");
       	}
       	
       	int value= (int) (5 + ( (x/25) * 245) ) ;
       	if( value > 250 | !growingBars)
       		value= 250;
       	
       	g.fillRect(i*90+70,50,40, value );
       	
    
       	
       	g.setColor(Color.white);
       	g.setFont(font);
       	
       	
       	g.drawString( "Z2:" , 10, 330);
       	g.drawString( Float.toString(EggDataArr[i][GlobalCounter]) , i*90+70, 330);
       	
      	int x2 = (int) EggDataArr[i][0];
      	g.drawString( "Egg:" , 10, 30);
       	g.drawString( Integer.toString(x2) , i*90+70, 30);
       }
       
       
       g.setColor(Color.white);
       g.setFont(font);
       
       g.drawString( stimeDate.toString() , 10, 380);
        stimeDate.setTime(stimeDate.getTime()+1000);
    }
    
}
