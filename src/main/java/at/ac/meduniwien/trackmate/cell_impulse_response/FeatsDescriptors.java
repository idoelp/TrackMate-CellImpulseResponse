package at.ac.meduniwien.trackmate.cell_impulse_response;

public enum FeatsDescriptors {
	conditionID,
	trackID,
	trackDuration, // frame wise
	trackStart, 
	trackStop, 
	trackDisplacement,  // overall features: 6       
	trackBurst, // frame-burst
	trackPeak, //frame-peak    
	framesBefore,
	framesAfter,// critical timePoints: 4 --> 10
	startX,
	startY,
	startZ,
	burstX, //x-burst 
	burstY, //y-burst
	burstZ, //z-burst
	stopX,
	stopY,
	stopZ,   // position at critical timepints: 9  --> 19
	velocityTotalMin,
	velocityTotalAv,
	velocityBeforeAv,
	velocityAfterAv,  // velocities at critical timepoints: 4 --> 23
	displTotal, // quality control, it should match trackDisplacement
	displBefore, 
	displAfter,  // displacement (3)	--> 26
	diameterAv,  
	diameterBeforeAv,
	diameterAfterAv,// shape (3) --> 29
	totalAv,
	total25,
	total50,
	total75,
	beforeAv,
	before25,
	before50,
	before75, 
	afterAv,
	after25, 
	after50, 
	after75,
	earlyAv,
	early25,
	early50,
	early75,
	lateAv,  
	late25,
	late50,
	late75// intensity (20)	--> 49
}
