package at.ac.meduniwien.trackmate.cell_impulse_response;

public enum FeatsTrackMate {
	// acquisition features
	SPOT_ID,
	RADIUS,
	QUALITY,
	SNR,
	CONTRAST,
	// informative features
	ESTIMATED_DIAMETER,
	POSITION_X,
	POSITION_Y,
	POSITION_Z // in case other trackmate feats are added, this should still be the last, because the intensities use Z channel as index reference
}

