//////////////////////////////////////
//	class EGGSample - a class that holds (transformations of) data of a simgle sample of
//	RNG points (a.k.a. Eggs, see EGG.java). Every second an egg creates a number of random
//	integers of 1's or 0's, called a sample. The number of integers is the trialsize per
//	sample. Eggsamples automatically compute the quadrated Z-score of the sample. It also has
//	a field that can be used to hold a "summed Z2", which is usefull when working with arrays
//	of EGGSamples.
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
//		JR 10/10/1998


class EGGSample{
	static final int		missingVal = 999;
	static final boolean 	debug = false;


	////////////////////////////////////// data variables
	int 	trialsz;	// number of trials (1/0's) in sample
	int		rawData;	// number of 1's in the trial
	int		summedR;	// sum of number of 1's in the trial over 60 seconds
	float 	Z2;			// squared Z-value of rawdata (if trialsize is known)
	float	summedZ2;	// used in arrays of EGGsamples
	int		df;
	int		df60;
	int 	sumCounter; // number of counts of rawdata
	float 	Z2_60;
	float 	summedZ2_60;

	////////////////////////////////////// Constructor
	//	r : integer count of 1's, tsz: integer count of total number of integers,
	//	lc: previous Eggsample that allows computation of summedZ2
	EGGSample(int r, int tsz, EGGSample lc){
		if( debug) new debug(Integer.toString(r));

		rawData 	= r;
		trialsz		= tsz > 0 ? tsz : 1;	// prevent NaN
		Z2 			= r!=missingVal ? ZSquared( r ) : missingVal;
		summedZ2	= (lc != null ?  ( r != missingVal ? lc.summedZ2 + Z2 - 1: lc.summedZ2 ) : 0 ) ;
		df = (lc != null) ? ( r!=missingVal? lc.df + 1 : lc.df ) : 0;

		summedR 	= (lc != null ? ( r!=missingVal? r + lc.summedR : lc.summedR) : 0 );
		sumCounter  = (lc != null ? ( r!=missingVal? lc.sumCounter + 1 : lc.sumCounter) : 0);


		if( sumCounter == 60) {
			sumCounter = 0;
			Z2_60 = (float) ( (float) ( (float) summedR - (float)60*(float)(trialsz/2) ) );
			Z2_60 = Z2_60 / (float) Math.sqrt(0.25*(float)trialsz*(float)60);
			summedZ2_60 = (float) (lc.summedZ2_60 + Math.pow( Z2_60 , 2 ) - 1);
			df60 = lc.df60 + 1;

			//System.out.println((float) summedR + "-" + (float)60*(float)(trialsz/2) + "=" + Z2_60 + " als / door" +   (float) Math.sqrt(0.25*(float)trialsz*(float)60));
			//System.out.println("resultaat " + (float)summedZ2_60);
			//System.out.println("********");

			summedR = 0;
		}
		else{
			summedZ2_60 = (lc != null ? lc.summedZ2_60 : 0);
			df60 = (lc != null ? lc.df60 : 0);
		}
	}

	////////////////////////////////////// Methods
	// ZSquared(): Calculate squared Z of rawData given trialsz
	public float ZSquared( int  v ){
		return (float) Math.pow( Z(v) , 2 );
	}
	// Z(): Calculate Z of rawData given trialsz
	public float Z( int  v ){
		return (float) ( (float) ( (float) v - (float) (trialsz/2) ) / Math.sqrt(0.25*(float)trialsz) );
	}

}
