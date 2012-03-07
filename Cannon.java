import processing.core.PApplet;
import processing.core.PVector;

public class Cannon
{
	/** The parent PApplet that we will render ourselves onto. */
	private PApplet p5;
	/** Position of the cannon at its base */
	private PVector position;
	/** Angle of the cannon from the ground-plane, from 0 to HALF_PI */
	private float angle;
	/** Force of the cannon, from 0 to 1.0f */
	private float impulseForceCoef;
	/** Maximal force of the cannon. */
	private float maxForce = 60.0f;
	/** Mass of the upcoming cannon-ball. */
	private float mass;
	/** Boolean indicating whether this is the human or the opponent cannon. */
	private boolean human;
	/** Length of the cannon. */
	public float length = 40.0f;
	
	Cannon(PApplet p, PVector position, boolean human) {
		this.p5 =p;
		this.position = position;
		this.human = human;
		this.angle = 0;
		pickRandomMass();
	}
	
	/**
	 * Obtain the position at the base of the cannon.
	 * @return PVector indicating the current position.
	 */
	public PVector getPosition() {
		return position.get();
	}
	
	/**
	 * Obtain the angle between the ground plane and the cannon.
	 * @return angle
	 */
	public float getAngle() {
		return angle;
	}
	
	/**
	 * Get the position at the center of the cannon used by the
	 * collision circle/body around it.
	 * @return Position PVector
	 */
	public PVector getCollisionCircleCenter() {
		float angle = this.angle+PApplet.HALF_PI; //Is is ok to always add HALF_PI here?
		angle = (human) ? angle : -angle;
		PVector center = new PVector(PApplet.sin(angle), PApplet.cos(angle));
		center.mult(length/2);
		center.add(position);
		return center;
	}
	
	/**
	 * Increase the cannon angle. If the angle is greater than HALF_PI,
	 * this has no effect.
	 * @param increment Angle increment
	 */
	public void increaseAngle(float increment) {
		angle += increment;
		angle = PApplet.constrain(angle, 0, PApplet.HALF_PI);
	}
	
	/**
	 * Decrease the cannon angle. If the angle is smaller than 0,
	 * this has no effect.
	 * @param increment Angle increment
	 */
	public void decreaseAngle(float increment) {
		angle -= increment;
		angle = PApplet.constrain(angle, 0, PApplet.HALF_PI);
	}
	
	/**
	 * Increase the force of the shot. If force is larger than 1.0f,
	 * this has no effect.
	 * @param increment Force increment
	 */
	public void increaseForce(float increment) {
		impulseForceCoef += increment;
		impulseForceCoef = PApplet.constrain(impulseForceCoef, 0, 1.0f);
	}
	
	/**
	 * Decrease the force of the shot. If force is smaller than 0.0f,
	 * this has no effect.
	 * @param increment Force increment
	 */
	public void decreaseForce(float increment) {
		impulseForceCoef -= increment;
		impulseForceCoef = PApplet.constrain(impulseForceCoef, 0, 1.0f);
	}
	
	/**
	 * Assign a random force to the cannon.
	 */
	public void pickRandomForce() {
		impulseForceCoef = p5.random(0.6f, 0.7f);
	}
	
	/**
	 * Assign a random mass to the next cannon-ball.
	 */
	private void pickRandomMass() {
		mass = p5.random(0.5f, 1.0f);
	}
	
	/**
	 * Shoot the cannon-ball. A body is added to the system.
	 * @param system System of (circular) bodies in which the new body is added.
	 * @return True if the cannon wasn't blocked and could shoot
	 *         (and consequently add a new body into the system).
	 */
	public boolean shoot(System system) {
		float angle = this.angle+PApplet.HALF_PI;
		angle = (human) ? angle : -angle;
		PVector cannonForce = new PVector(PApplet.sin(angle), PApplet.cos(angle));
		Body b1 = new Body(p5, PVector.add(position, PVector.mult(cannonForce, length)), mass, human);
		cannonForce.mult(impulseForceCoef*maxForce);
		impulseForceCoef = 0.0f;
		pickRandomMass();
		if (!system.inCollision(b1)) {
			b1.addForce(cannonForce);
			system.addBody(b1);
			return true;
		}
		return false;
	}
	
	/**
	 * Draw the upcoming cannon-ball and cannon force.
	 */
	private void drawInfo() {
		p5.fill((human) ? Constants.humanColor: Constants.opponentColor);
		//Force bar
		int length = (int) (impulseForceCoef*maxForce);
		p5.pushMatrix();
		if (human) {
			p5.translate(15, p5.height-20);
		} else {
			p5.translate(p5.width-100, p5.height-20);
		}
		p5.quad(0, 0, 0, -10, length, -10, length, 0);
		p5.popMatrix();
		
		//Mass preview
		p5.pushMatrix();
		if (human) {
			p5.translate(20, p5.height-50);
		} else {
			p5.translate(p5.width-20, p5.height-50);
		}
		p5.ellipse(0, 0, 2*Constants.massToRadiusRatio*mass, 2*Constants.massToRadiusRatio*mass);
		p5.popMatrix();
	}
	
	/**
	 * Draw the cannon as a simple quad.
	 */
	public void draw() {
		p5.fill((human) ? Constants.humanColor: Constants.opponentColor);
		p5.pushMatrix();
		p5.translate(position.x, position.y);
		p5.rotate((human) ? -angle: angle);
		p5.quad(0, 0, 0, -10, (human)?length:-length, -10, (human)?length:-length, -2);
		p5.popMatrix();
		drawInfo();
	}
	
	/**
	 * Draw the broken cannon as a simple quad, outlined by a larger yellow one.
	 */
	public void drawBroken() {
		p5.pushMatrix();
		p5.translate(position.x, position.y);
		p5.rotate((human) ? -angle: angle);
		p5.fill(p5.color(255, 255, 60));
		p5.quad(0, 5, 0, -15, (human)?length+5:-length-5, -15, (human)?length+5:-length-5, 5);
		p5.fill((human) ? Constants.humanColor: Constants.opponentColor);
		p5.quad(0, 0, 0, -10, (human)?length:-length, -10, (human)?length:-length, -2);
		p5.popMatrix();
		drawInfo();
	}
}
