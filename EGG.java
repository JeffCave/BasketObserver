//////////////////////////////////////
//	class EGG - EGGs are individual sample-points of RNG's. They keep track of their own
//	samplehistory, and have two different paint methods to display the data:
//
//	1: Z2-Data of sample t is displayed as a dynamic vertical block with a specific color.
//	2: History of summed Z2-Data of samples (ti-offset) to ti is displayed in a "vertical signal plot" format.
//
//	Refer to http://noosphere.princeton.edu/ for more information about
//	the EGG project.
//
//	Usage:
//		int trialsize     = 200;
//		int	NumberOfOnes  = 100;
//		EGGSample sample  = new EGGSample(NumberOfOnes,trialsize,null);
//		EGGSample sample2 = new EGGSample(NumberOfOnes,trialsize,sample);
//
//	remarks:
//		JR 10/10/1998:
//			This was a learning project, so the implementation may seem "twisted" to experienced
//			Java programmers.


import java.applet.*;
import java.awt.Panel;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.Color;
import java.awt.Canvas;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Event;
import java.util.Hashtable;
import java.util.Date;

class EGG extends Panel {
	////////////////////////////////////// class variables
	static final boolean	debug = false;		// set to true for debug information
	static final int		PAINT_CURRENT = 1;	// display current Z2-value
	static final int		PAINT_HISTORY = 2;	// display history of Z2-values
	static final int		PAINT_HISTORY60 = 3;	// display history of Z2-values
	static final int		DEFAULT_PAINT = PAINT_CURRENT;
	static final int		missingVal = 999;
	private Applet applet;

	////////////////////////////////////// GUI Components
	Label					label;				// title of egg display (constant)
	Canvas					canvas;				// canvas to draw in
	Button					button;				// button for display type

	////////////////////////////////////// Internal data representation
	String 					lbl;				// label of Egg
	Hashtable				data;				// EggSample Objects hashed by UTC string

	//////////////////////////////////////  Fonts & Colors
	Font 					bigFont;			// Font used for displays
	Font 					smallFont;			// Font used for displays
	Color					bgColor;			// background color
	Color					animBgColor;		// background color for animation display
	Color					fgColor;			// foreground color

	//////////////////////////////////////  Animation Buffers
	private Image 			offscreen1;			// offscreen Imagebuffer #1 (for PAINT_CURRENT)
	private Image 			offscreen2;			// offscreen Imagebuffer #2 (for PAINT_HISTORY)
	private Image 			offscreen3;			// offscreen Imagebuffer #3 (for PAINT_HISTORY60)

	//////////////////////////////////////  private variables
	private Date			lastDate;			// last Date animated by the Egg
	private int				curr_display;		// current display type: PAINT_CURRENT/PAINT_HISTORY
	private int				Old_X;				// last X value in PAINT_HISTORY
	private int				Old_X2;				// last X value in PAINT_HISTORY60
	private int				imagewidth;			// width of canvas display
	private int				imageheight;		// heigth of canvas display
    private float             savedScale;         // saved scaling factor of cumulated secs display


	////////////////////////////////////// Constructor
	//	label: namestring of Egg,app: applet under control of the egg
	EGG(String label, Applet app){
		if(debug) new debug("Egg: constructor");

		applet = app;				// we need to know the super applet, so we can play sounds
		lbl = label;				// label of the egg (used as key in basketobserver.class)
		data = new Hashtable();		// samples are kept in a hastable, keyed by stringed dates
        
        savedScale = 1;
        
		initFonts();
		initColors();
		initDimensions();
		initComponents();
		initAnimation();

	}

	////////////////////////////////////// Methods

	////////////////////////////////////// Initialization Methods
	// initFonts (): initialize display fonts
	public void initFonts(){
		if(debug) new debug("Egg: initFonts()");

		bigFont   = new Font("Courier", Font.PLAIN, 12);
		smallFont = new Font("Courier", Font.PLAIN, 10);
	}
	// initColors (): initialize foreground/background colors
	public void initColors(){
		if(debug) new debug("Egg: initColors()");

		bgColor = Color.darkGray;
		animBgColor  = Color.gray;

		this.setBackground(bgColor);
		fgColor = Color.white;
		setForeground(fgColor);

	}
	// initDimensions (): initialize display width/height so we know when it changes
	void initDimensions(){
		if(debug) new debug("Egg: initDimensions()");

		Dimension d = this.size();
		imagewidth  = d.width;
		imageheight = d.height;

	}
	// initComponents (): initialize all display components
	void initComponents(){
		if(debug) new debug("Egg: initComponents()");

		// layout of main panel
		this.setLayout(new BorderLayout());

		// title at top of panel displays Egg label
		label = new Label("Egg: " + lbl,Label.CENTER);
		label.setFont(smallFont);
		this.add("North", label);

		// canvas at centre of panel is where the animation is drawn
		canvas = new Canvas();
		canvas.setBackground(animBgColor);
		this.add("Center", canvas);

		// display type button at bottom of panel
		switch(DEFAULT_PAINT){
			case PAINT_CURRENT: button = new Button("Current"); break;
			case PAINT_HISTORY: button = new Button("Cum. secs"); break;
			case PAINT_HISTORY60: button = new Button("Cum. mins"); break;
		}
		button.setForeground(Color.white);
		button.setBackground(Color.darkGray);
		this.add("South", button);

	}
	// initAnimation (): initialize variables for animation
	public void initAnimation(){
		if(debug) new debug("Egg: initAnimation()");

		curr_display = DEFAULT_PAINT;						// set default display type

		Dimension d = canvas.size();
		offscreen1 = canvas.createImage(d.width, d.height);	// offscreen Graphics buffer 1
		offscreen2 = canvas.createImage(d.width, d.height);	// offscreen Graphics buffer 2
		offscreen3 = canvas.createImage(d.width, d.height);	// offscreen Graphics buffer 3

		lastDate = null;
	}


	////////////////////////////////////// Event methods
	// action (): what happens when the button is clicked?
	public boolean action(Event event, Object arg) {
		if(debug) new debug("Egg: action()");

		if( event.target instanceof Button){
			if( event.target == button ) {
				if( button.getLabel().equals("Current") ){
					setDisplayType(PAINT_HISTORY);
				}
				else{
					if( button.getLabel().equals("Cum. secs") ){
						setDisplayType(PAINT_HISTORY60);
					}
					else{
						setDisplayType(PAINT_CURRENT);
					}
				}
			}
			return true;
		}
		else return false;
	}
	// setDisplayType (): set animation variables to the animation of type "type"
	// type: one of the integer constant animation type labels
	public void setDisplayType(int type){
		if(debug) new debug("Egg: setDisplayType()");

		curr_display = type;
		switch( type ){
  			case PAINT_CURRENT: button.setLabel("Current"); break;
  			case PAINT_HISTORY: button.setLabel("Cum. secs"); break;
  			case PAINT_HISTORY60: button.setLabel("Cum. mins"); break;
  		}
	}


	////////////////////////////////////// Animation Methods
	// animate(d): calculate the animated image of the sample of eggdata at date "d" to an offscreen image
	// d: date by which data is hashed in the Egg
	void animate(Date d){
		if(debug) new debug("Egg: animate()");

		// animate Current in offscreen image #1
		Graphics g = getOffscreenGraphics(1);	// get the graphics port of offscreen image #1
		drawBackground(g);						// clear it
		paintCurrent(g, d); 					// and paint new block

		// animate History in offscreen image #2
		g = getOffscreenGraphics(2);			// get the graphics port offscreen image #2
		paintHistory(g, d);						// paint history

		g = getOffscreenGraphics(3);			// get the graphics port offscreen image #2
		paintHistory60(g, d);					// paint history

		lastDate = new Date(d.getTime());

	}
	// paintCurrent(g,d): draw a colored block with a height proprotional to the sample value at date d
	// g: graphics port to draw in,  d: date by which data is hashed in the Egg
   	public void paintCurrent(Graphics g, Date d){
   		if(debug) new debug("Egg: paintCurrent()");

   		// get the value from the data hashtable by key datestring
	    EGGSample sample = (EGGSample) data.get(d.toString());

	    if(debug) new debug(Float.toString( sample.Z2 ) + " " + d.toString());

	    if( sample.Z2 != missingVal) {
		    // translate value to screen value
	  		Dimension size = canvas.size();
	  		int value= (int) (12 + ( (sample.Z2/25) * (size.height - 20) ) ) ;	// determine height
	   		if( value > (size.height - 20) ) value= (size.height - 20);				// bounds checking

	  		// set color of the block as determined by value
	   		Color c = new Color ( 	(int) sample.Z2 < 9 ? 75 + (int) sample.Z2*20 : 255,
	   								(int) sample.Z2 < 16 ? 0 : ( (int) sample.Z2 < 25 ? 75 + (int) (sample.Z2-16)*20 : 255 ),
	   								0
	   					  		);

	   		g.setColor(c);

	  		// draw the block
	  		g.fillRect(10,12,size.width-20, value );					// draw it

	  		// play sounds based on value
	  		if( sample.Z2 > 4 & sample.Z2 < 9 ) {
   				applet.play(applet.getDocumentBase(), "1.au");
   			}
   			if( sample.Z2 >= 9 & sample.Z2 < 16 ) {
   				applet.play(applet.getDocumentBase(), "2.au");
   			}
   			if( sample.Z2 >= 16 & sample.Z2 != missingVal) {
   				applet.play(applet.getDocumentBase(), "3.au");
   			}

  		}

  		g.setColor(Color.white);

  		if( sample.Z2 == missingVal) {
  			g.drawString( "unavailable", 10, 10 );
  		}
  		else {
  		// draw the Value as number
  		    float displayValue = Math.round(sample.Z2 * 1000);
   		    displayValue = displayValue / 1000;
  			g.drawString( Float.toString(displayValue), 10, 10 );
  		}
   	}
   	// paintHistory(g,d): draw a "signal-like" plot with sample value at date d at the bottom
	// g: graphics port to draw in,  d: date by which data is hashed in the Egg
   	public void paintHistory (Graphics g, Date d) {


   		// drawing is based on the size of the canvas
   		Dimension size = canvas.size();
   		//static int Old_X = (int) size.width/2;
   		int x= (int) size.width/2;
   		int dy = 3;

   		// every value moves the plot three pixels down
   		int y= (int) size.height-dy;
         
         
        // get the value from the data hashtable by key datestring
   		EGGSample sample = (EGGSample) data.get(d.toString());

  		// translate value to screen value
  		float value= (sample.summedZ2);
  		
  		// debug code
  		//if( value != missingVal)
  		//   value *= 1;
  		
  		// determine scaling
        float scale = savedScale;
        // do we have to scale down?
        if( ((Math.abs (savedScale * Math.abs(value)) > (x - 5)) | (savedScale == 0)) & (value != missingVal) ){
           if( value != 0 && value != missingVal){
              scale = (float) x / (float) Math.abs( value ) ;
              if( scale >= 1) scale = 1;
              else {
                scale = (float) Math.pow(scale, 2);
              }
           }
           savedScale = scale;
        }
        // do we have to scale up?
        if( (savedScale * Math.abs(value)) < (x/20) ){
           if( value != 0 && value != missingVal){
              scale = (float) x / (float) Math.abs( value ) ;
              if( scale >= 1) scale = 1;
              else {
                scale = (float) Math.pow(scale, 2);
              }
           }
           savedScale = scale;
        }
       
        // determine glow by scale
        int Glow  = 255 - (int) Math.floor(scale * 100 );
        // set color of the background as determined by Glow
   		Color c1 = Color.darkGray;
   		if( Glow != 0){
   		    c1 = new Color(Glow,0,Glow); 
        }
        
   		// scroll currentimage dy pixels up
   		g.drawImage(offscreen2, 0,-dy,this);
   		g.setColor(c1);
   		g.fillRect(0,y,size.width,dy);
   					  		
   		// paint value	  		
   		g.setColor(Color.gray);
   		g.fillRect(0,0, size.width,10);
   		g.setColor(Color.white);
   		float displayValue = Math.round(value * 1000);
   		displayValue = displayValue / 1000;
   		g.setColor(Color.white);
		g.drawString( Float.toString(displayValue) + " d:" + 
Integer.toString(sample.df), 10, 10 );

   		// draw baseline axis based on scale
   		g.setColor(Color.black);
   		g.drawLine( x, size.height-4, x, size.height);

        int value2 = 0;
   		if( value == missingVal) {
	   		// set color of the line to black
	   		g.setColor(Color.white);
   		    value2 = Old_X;
   		}
   		else {
   			// set color of the line as determined by value
	   		value2 = (int) Math.floor(value * scale);
	   		int value3 = (int) Math.abs(value2);
	   		Color c = new Color ( 	(int) value3 < 9 ? 75 + (int) value3*20 : 255,
	   								(int) value3 < 16 ? 0 : ( (int) value3 < 
25 ? 75 + (int) (value3-16)*20 : 255 ),
	   								0
	   					  		);

	   		g.setColor(c);

	   		// play sounds based on value
	   		if( sample.Z2 > 4 & sample.Z2 < 9 ) {
   				applet.play(applet.getDocumentBase(), "1.au");
   			}
   			if( sample.Z2 >= 9 & sample.Z2 < 16 ) {
   				applet.play(applet.getDocumentBase(), "2.au");
   			}
   			if( sample.Z2 >= 16 & sample.Z2 != missingVal) {
   				applet.play(applet.getDocumentBase(), "3.au");
   			}
   		}

   		// draw line, save current value in Old_X
   		if( value2 == Old_X){
   		    g.drawLine( Old_X, y, Old_X, y+dy);    
   		}
   		else {
   		    g.drawLine( Old_X, y, (Old_X = x + (int) Math.floor( (value2)) ) , y+dy);
        }
   		// prevent out of screen plot
   		//if( ((x+value) < 5) || ((x+value) > (size.width - 5)) )
   		//	g.drawLine( Old_X, y, Old_X , y+dy);
   		//else
   		//	g.drawLine( Old_X, y, (Old_X = x + (int) Math.floor( scale * (value)) ) , y+dy);

   	}

  	// paintHistory60(g,d): draw a "signal-like" plot with sample value at date d at the bottom
	// g: graphics port to draw in,  d: date by which data is hashed in the Egg
   	public void paintHistory60 (Graphics g, Date d) {


   		// get the value from the data hashtable by key datestring
   		EGGSample sample = (EGGSample) data.get(d.toString());
  		if( sample.sumCounter != 1) return;


  		// drawing is based on the size of the canvas
   		Dimension size = canvas.size();
   		//static int Old_X = (int) size.width/2;
   		int x= (int) size.width/2;
   		int dy = 3;

   		// every value moves the plot three pixels down
   		int y= (int) size.height-dy;

   		// scroll currentimage dy pixels up
   		g.drawImage(offscreen3, 0,-dy,this);
   		g.setColor(Color.gray);
   		g.fillRect(0,y,size.width,dy);

  		// translate value to screen value
  		int value= (int) (sample.summedZ2_60);
   		g.setColor(Color.darkGray);
   		g.fillRect(0,0, size.width,10);
   		g.setColor(Color.white);
   		
   		float displayValue = Math.round(sample.summedZ2_60 * 1000);
   		displayValue = displayValue / 1000;
   		g.setColor(Color.white);
		g.drawString( Float.toString(displayValue) + " (" + 
Integer.toString(sample.df60) + ")", 10, 10 );

   		// draw baseline
   		g.setColor(Color.black);
   		g.drawLine( x, size.height-4, x, size.height);
        
		// set color of the line as determined by value
   		int value2 = Math.abs(value);
   		Color c = new Color ( 	(int) value2 < 9 ? 75 + (int) value2*20 : 255,
   								(int) value2 < 16 ? 0 : ( (int) value2 < 25 ? 75 + (int) (value2-16)*20 : 255 ),
   								0
   					  		);

   		g.setColor(c);


   		// draw line, save current value in Old_X
   		// prevent out of screen plot
   		if( ((x+value) < 5) || ((x+value) > (size.width - 5)) )
   			g.drawLine( Old_X2, y, Old_X2 , y+dy);
   		else
   			g.drawLine( Old_X2, y, Old_X2 = ( x+value)  , y+dy);

   	}


   	// reveal(): copy offscreen image to the screen
	void reveal(){
		if(debug) new debug("Egg: reveal()");

		Graphics g = canvas.getGraphics();
		switch( curr_display ){

  			case PAINT_CURRENT: g.drawImage(offscreen1,0,0,this);
  								break;

  			case PAINT_HISTORY:	g.drawImage(offscreen2,0,0,this);
  								break;

  			case PAINT_HISTORY60:	g.drawImage(offscreen3,0,0,this);
  									break;
  		}
	}

   	////////////////////////////////////// Animation Utility Methods
	// getOffscreenGraphics(which): returns the graphics port of the offscreen image #"which"
	// integer "which" denotes the index of the requested offscreen image (#1 or #2)
	public Graphics getOffscreenGraphics(int which){
   		if(debug) new debug("Egg: getOffscreenGraphics()");

   		Graphics g = null;

   		checkDimensions(); // create new offscreen Image buffers when panel dimensions are changed

		switch( which ){

  			case 1: g = offscreen1.getGraphics();
  					break;

  			case 2:	g = offscreen2.getGraphics();
  					break;

  			case 3:	g = offscreen3.getGraphics();
  					break;
  		}

		return g;
	}
	// checkDimensions(): is the offscreen image still valid?
	// if the size of the panel is changed, new offscreen images with the right dimensions
	// are created
	void checkDimensions(){
		if(debug) new debug("Egg: checkDimensions()");

		Dimension d = canvas.size();
		if( (imagewidth != d.width) || (d.height != imageheight)) {
			offscreen1 = this.createImage(d.width, d.height);
			offscreen2 = this.createImage(d.width, d.height);
			offscreen3 = this.createImage(d.width, d.height);
			imagewidth = d.width;
		 	imageheight = d.height;
			Old_X = (int) (d.width/2);	// initial x-coordinate for history animation
			Old_X2 = (int) (d.width/2);	// initial x-coordinate for history animation

		}
	}
	// drawBackground(g): draws a clear background on graphics port "g"
	// used to clear previous animation-results
	void drawBackground(Graphics g){
		if(debug) new debug("Egg: drawBackground()");

	   	Dimension size = canvas.size();
	   	g.setColor( animBgColor );
	   	g.fillRect(0,0,size.width,size.height);
	}

} // end of class
