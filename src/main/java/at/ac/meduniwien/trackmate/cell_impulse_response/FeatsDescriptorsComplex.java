package at.ac.meduniwien.trackmate.cell_impulse_response;

public enum FeatsDescriptorsComplex {
	conditionContextID, // conditionName.hashCode()
	specimenID, // specimenName.hashCode()
	isIncluded, // gating mask: 1 is valid, 0 is invalid/excluded from analysis
	isActive, // activation based on parameter P > threshold, where threshold: f(µ_negCtrl,σ_negCtrl)
	totalAvNorm,
	total25Norm,
	total50Norm,
	total75Norm,
	beforeAvNorm,
	before25Norm,
	before50Norm,
	before75Norm, 
	afterAvNorm,
	after25Norm, 
	after50Norm, 
	after75Norm,
	earlyAvNorm,
	early25Norm,
	early50Norm,
	early75Norm,
	lateAvNorm,  
	late25Norm,
	late50Norm,
	late75Norm,	
    intensityAvHi,  // dynamic parameters
    intensityAvLo,
    ratioIntensityHiLo,
    velocityAvHi,
    velocityAvLo,
    ratioVelocityHiLo,
    diameterAvHi,
    diameterAvLo,
    ratioDiameterHiLo,
    framesHi, 
    framesLo,
    proportionFramesHiLo, 
	startsHi, // kinetic parameters
    endsHi,
    stateSwitches,
    numberHiStates,
    numberLoStates,
    avDurationHi,
    avDurationLo,
    maxDurationHi,
    maxDurationLo,
    lastHiStateDuration
}
