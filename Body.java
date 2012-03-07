import processing.core.PApplet;
import processing.core.PVector;

public class Body
{
	/** The parent PApplet that we will render ourselves onto. */
	private PApplet p5;
	/** Body index */
	public int index;
	/** Bolean indicating whether the body has already collided with the terrain or not. */
	public boolean hasCollidedTerrain;
	/** Position */
	public PVector pos;
	/** Last position (verlet integration) */
	public PVector ppos;
	/** Current acceleration */
	public PVector acc;
	/** Radius & mass values */
	public float initRad, rad, mass;
	/** Owned by the human or by the opponent? */
	public boolean human;
	/** Inverted mass */
	private float invMass;
	/** Color */
	private int col;

	private int shockCol;

	Body(PApplet p, PVector pos, float mass, boolean human)
	{
		this.p5 = p;
		this.human = human;
		this.pos = new PVector();
		this.ppos = new PVector();
		this.acc = new PVector();
		this.hasCollidedTerrain = false;
		this.index = System.bodyIndex;
		this.pos.set(pos);
		this.ppos.set(pos);
		this.mass = mass;
		this.invMass = 1.0f/(1.0f+mass); //non-standard, need to tweak values
		this.rad = Constants.massToRadiusRatio*mass;
		this.initRad = rad;
		this.col = (human) ? Constants.humanColor: Constants.opponentColor;
		this.shockCol = 255;
	}
	
	/**
	 * Set the body radius.
	 * @param rad Radius.
	 */
	public void setRadius(float rad) {
		this.rad = rad;
	}
	
	/**
	 * Obtain the separation between the bodies' positions.
	 * @param b Separated body
	 * @return Length of the separation
	 */
	public float separation(Body b)
	{
		return this.pos.dist(b.pos);
	}
	/**
	 * Obtain the separation between the bodies' positions.
	 * @param pos Compared position
	 * @return Length of the separation
	 */
	public float separation(PVector pos)
	{
		return this.pos.dist(pos);
	}

	/**
	 * Minimum allowed separation between the bodies.
	 * @param b Separated body
	 * @return Minimum distance
	 */
	public float minSeparation(Body b)
	{
		return rad + b.rad;
	}

	/**
	 * Add force to the body.
	 * @param force
	 */
	public void addForce(PVector force)
	{
		acc.add(force);
	}

	/**
	 * Move the body by one simulation step.
	 * @param dt Time interval.
	 * @param shockFactor Increases the radius
	 */
	public void step(float dt, float shockFactor)
	{
		rad = initRad + 0.35f * initRad * shockFactor;
		pos.add(PVector.mult(acc, invMass*dt * dt));
		acc.set(0, 0, 0);
	}

	/**
	 * Preserve the body inertia.
	 */
	public void preserveInertia()
	{
		PVector temp = PVector.mult(pos, 2);
		temp.sub(ppos);
		ppos.set(pos);
		pos.set(temp);
	}

	public void draw(float shockFactor)
	{
		p5.noStroke();
		p5.fill(p5.lerpColor(col, shockCol, shockFactor));
		p5.ellipse(pos.x, pos.y, 2 * rad, 2 * rad);
	}
}
