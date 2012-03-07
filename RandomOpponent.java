import javax.swing.Timer;
import java.awt.event.*;

import processing.core.PApplet;

public class RandomOpponent
{
	/** The parent PApplet that we will render ourselves onto. */
	PApplet p5;
	/** Cannon reference controlled by this opponent. */
	Cannon cannon;
	/** Cannon angle variable regularly modified. */
	float newAngle;
	/** Timer defining the interval at which the cannon shoots. */
	Timer timer;
	/** Reference to the system of (circular) bodies. */
	final System systemRef;
	
	RandomOpponent(PApplet p, Cannon cannon, System system) {
		this.p5 = p;
		this.cannon = cannon;
		this.systemRef = system;
		this.newAngle = p5.random(0, 0.75f*PApplet.HALF_PI);
		timer = new Timer(2000, new ActionListener() {
	          public void actionPerformed(ActionEvent e) {
	        	  shoot();
	          }
	    });
	}
	
	/**
	 * Reset the opponent
	 * @param cannon New cannon reference.
	 */
	public void reset(Cannon cannon) {
		this.cannon = cannon;
	}
	
	/**
	 * Start the opponent by enabling its timer.
	 */
	public void enable() {
		timer.start();
	}
	
	/**
	 * Stop the opponent by disabling its timer.
	 */
	public void disable() {
		timer.stop();
	}
	
	/**
	 * Shoot a cannon-ball.
	 */
	private void shoot() {
		cannon.pickRandomForce();
		cannon.shoot(systemRef);
		newAngle = p5.random(0, 0.75f*PApplet.HALF_PI);
	}
	
	/**
	 * Automate the cannon tilt modifications.
	 */
	public void step() {
		float currentAngle = cannon.getAngle();
		if (currentAngle < newAngle) {
			cannon.increaseAngle(0.03f);
			systemRef.setCannonCollisionPosition(cannon.getCollisionCircleCenter(), false);
		} else if (currentAngle > newAngle){
			cannon.decreaseAngle(0.03f);
			systemRef.setCannonCollisionPosition(cannon.getCollisionCircleCenter(), false);
		}
	}
}
