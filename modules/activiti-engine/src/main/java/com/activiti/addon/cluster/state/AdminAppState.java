package com.activiti.addon.cluster.state;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jbarrez
 */
public class AdminAppState {
	
	protected Logger logger = LoggerFactory.getLogger(AdminAppState.class);
	
	public static long timeLimit = 300000L; // 5 minutes (admin app sends every minute keepAlive message)
	
	public enum State { ALIVE, DEAD };
	
	protected Date lastIAmAliveEventTimeStamp = new Date(); // When booting up, assume it's alive and send events until time limit is passed
	
	protected State state = State.ALIVE;
	
	public void iAmAliveEventReceived() {
		lastIAmAliveEventTimeStamp = new Date();
		
		if (state.equals(State.DEAD)) {
			logger.info("Admin app was not reachable by this node, but now received an 'i am alive' event.");
			setState(State.ALIVE);
		}
	}

	public State getState() {
		Date now = new Date();
		if (now.getTime() - lastIAmAliveEventTimeStamp.getTime() >= timeLimit) {
			setState(State.DEAD);;
		}
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
	
}
