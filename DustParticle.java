import processing.core.PApplet;
import processing.core.PVector;

public class DustParticle
{
	/** The parent PApplet that we will render ourselves onto. */
	private PApplet p5;
	/** Position */
	private PVector pos;
	/** Force accumulator acting on the particle */
	private PVector force;
	/** List of precedent positions for drawing a trail */
	private PVector trail[];
	/** Number of positions in the trail */
	private int sizeTrail;
	/** Current particle velocity */
	private PVector vel;
	/** Particle mass */
	private float mass;
	/** Noise increment */
	private float zincrement;
	
	DustParticle(PApplet p, PVector position, float mass) {
		this.p5 = p;
		this.pos = position;
		this.vel = new PVector();
		this.force = new PVector();
		this.mass = mass;
		this.zincrement = 0.01f;
		this.sizeTrail = 10;
		this.trail = new PVector[sizeTrail];
		for(int i=0; i<sizeTrail; ++i) {
			this.trail[i] = pos.get();
		}
	}
	
	/**
	 * Animate the dust trails by one step.
	 * @param zoff Noise offset for fluid motion.
	 * @param windForce Lateral wind force affecting the motion.
	 */
	void step(float zoff, PVector windForce) {
		float pxoff = zincrement* pos.x;
		float pyoff = zincrement* pos.y;
	    float nval = (p5.noise(pxoff, pyoff, zoff)-0.5f)*2.0f*PApplet.TWO_PI;
	    force.x = PApplet.cos(nval)*mass;
	    force.y = PApplet.sin(nval)*mass;
	    force.mult(0.1f);
	    vel.add(force);
	    vel.add(PVector.mult(vel, -0.025f));
	    vel.add(PVector.mult(windForce, 1.8f));
	    pos.add(vel);
	    postStep();
	}
	
	/**
	 * Post-step fix. Loop the particles around the edges.
	 */
	void postStep() {
		int border = 7;
		if(pos.x < -border) {
			pos.x = p5.width;
			for(int i=sizeTrail-1; i>0; --i) {
				trail[i] = pos.get();
			}
		} else if (pos.x > p5.width+border) {
			pos.x = 0;
			for(int i=sizeTrail-1; i>0; --i) {
				trail[i] = pos.get();
			}
		}
		if(pos.y < -border) {
			pos.y = p5.height;
			for(int i=sizeTrail-1; i>0; --i) {
				trail[i] = pos.get();
			}
		} else if (pos.y > p5.height+border) {
			pos.y = 0;
			for(int i=sizeTrail-1; i>0; --i) {
				trail[i] = pos.get();
			}
		}
		trail[0] = pos.get();
		for(int i=sizeTrail-1; i>0; --i) {
			trail[i] = trail[i-1].get();
		}
	}
	
	void draw() {
		p5.stroke(255);
		p5.strokeWeight(1.0f);
		for(int i=0; i<sizeTrail-1; ++i) {
			p5.line(trail[i].x, trail[i].y, trail[i+1].x, trail[i+1].y);
		}
		p5.noStroke();
	}
}
