//////////////////////////////////////
//	class debug - a utility class for debugging.
//	prints a msg to System.out
//
//	Usage:
//
//		boolean debug = true;
//		if( debug) debug("test");
//
//	remarks:
//		JR 10/10/1998


class debug {

	debug(String msg){
		System.out.println("debug: " + msg);
	}

}
