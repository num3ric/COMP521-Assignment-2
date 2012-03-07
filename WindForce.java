import processing.core.PApplet;
import processing.core.PVector;

public class WindForce
{
	/** The parent PApplet that we will render ourselves onto. */
	private PApplet p5;
	/** Force of the wind */
	private PVector force;
	/** Varying time which will affect the force */
	private float time;
	/** Size of the time increment affecting the speed of change.*/
	private float stepSize;

	WindForce(PApplet p) {
		this.p5 = p;
		time = p5.random(PApplet.TWO_PI);
		force = new PVector(0,0);
		stepSize = 0.004f;
	}
	
	/**
	 * Increment the force in time.
	 */
	public void step()
	{
		time += stepSize;
		force.x = Constants.maxWindForceMagnitude*PApplet.sin(time);
	}

	/**
	 * Obtain the lateral wind force.
	 * @return Wind force PVector 
	 */
	public PVector getForce()
	{
		return force.get();
	}
	
	/** Draw an arrow.
	 * @param x1 Start x-coord
	 * @param y1 Start y-coord
	 * @param x2 End x-coord
	 * @param y2 End y-coord
	 */
	private void arrow(float x1, float y1, float x2, float y2) {
		p5.line(x1, y1, x2, y2);
		p5.pushMatrix();
		p5.translate(x2, y2);
		float a = PApplet.atan2(x1-x2, y2-y1);
		p5.rotate(a);
		p5.line(0, 0, -6, -6);
		p5.line(0, 0, 6, -6);
		p5.popMatrix();
	} 

	/** Draw the wind force as an arrow */
	public void draw() {
		p5.strokeWeight(3.0f);	
		p5.stroke(0, 150, 0);
		float x1 = p5.width - 75.0f;
		float y1 = 10.0f;
		float x2 = x1 + 300.0f*force.x;
		float y2 = y1;
		arrow(x1, y1, x2, y2);
		p5.noStroke();
	}
}
