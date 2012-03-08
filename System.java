import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;
// Used as a reference for verlet collision response between circles
// http://codeflow.org/entries/2010/nov/29/verlet-collision-with-impulse-preservation/
public class System
{
	/** The parent PApplet that we will render ourselves onto. */
	private PApplet p5;
	/** List of bodies used for collision. */
	private ArrayList<Body> bodies;
	/** Number of dust particles moving from the wind force. */
	private int nbDustParticles;
	/** Noise z-offset value for the dust particles fluid motion. */
	private float zoff = 0.0f; //
	/** Noise increment for the dust particles */
	private final float zIncrement = 0.002f;
	/** Array of dust particles */
	private DustParticle dustParticles[];
	/** Gravity force acting downward on bodies. */
	private PVector gravity;
	/** Time-varying wind force acting laterally on bodies. */
	private WindForce wind;
	/** Trigger shock in the system to test stability. */
	private float shockFactor = 0;
	/** Bodies used for detecting collisions with cannons. */
	private Body cannonCollision1, cannonCollision2;
	/** Boolean indicating whether border constraints is activated */
	private boolean useBorderConstraint;
	/** Boolean indicating whether cannon collision is activated */
	private boolean useCannonCollision;
	/** Index of the current body being added to the system */
	static int bodyIndex = 0;

	System(PApplet p)
	{
		this.p5 = p;
		this.gravity = new PVector(0, Constants.gravityFactor, 0);
		this.bodies = new ArrayList<Body>();
		wind = new WindForce(p5);
		nbDustParticles = 40;
		dustParticles = new DustParticle[nbDustParticles];
		for(int i=0; i<nbDustParticles; ++i) {
			dustParticles[i] = new DustParticle(p5,
					new PVector(p5.random(p5.width), p5.random(p5.height) ),
					p5.random(0.5f, 1.0f));
		}
		this.useBorderConstraint = false;
		this.useCannonCollision = true;
	}
	/** 
	 * Add cannon bodies to the system.
	 * @param center1 Center of the first(human) cannon
	 * @param rad1 Radius of the first(human) cannon
	 * @param center2 Center of the second (opponent) cannon
	 * @param rad2 Radius of the second (opponent) cannon
	 */
	public void setCannonCollisions(PVector center1, float rad1, PVector center2, float rad2) {
		this.cannonCollision1 = new Body(p5, center1, 1.0f, true);
		this.cannonCollision1.setRadius(rad1);
		this.cannonCollision2 = new Body(p5, center2, 1.0f, false);
		this.cannonCollision1.setRadius(rad2);
	}

	/**
	 * Add (circular) body to the system for motion and collision.
	 * @param body Body to be added.
	 */
	public void addBody(Body body){
		bodies.add(body);
		bodyIndex++;
	}
	
	/**
	 * Adds a body only if it does not collide with others.
	 * @param body Body to be added.
	 * @return True if the body was added.
	 */
	public boolean safeAddBody(Body body) {
		if (!inCollision(body)) {
			bodies.add(body);
			bodyIndex++;
			return true;
		}
		return false;
	}

	/**
	 * Reset to default and remove all bodies.
	 */
	public void reset() {
		bodies.clear();
		bodyIndex = 0;
		useBorderConstraint = false;
		useCannonCollision = true;
	}

	/**
	 * Get the number of (circular) bodies.
	 * @return int quantity
	 */
	public int getNumberOfBodies() {
		return bodies.size();
	}
	
	/**
	 * Enable border constraints (for demonstration)
	 * (The bodies do no 'die' if they touch water.
	 * @param enable
	 */
	public void useBorderConstraint(boolean enable) {
		useBorderConstraint = enable;
	}
	
	public void useCannonCollision(boolean enable) {
		useCannonCollision = enable;
	}

	/**
	 * Change the gravity angle.
	 * @param angle New angle.
	 */
	public void reorientGravity(float angle)
	{
		gravity.x = Constants.gravityFactor * PApplet.sin(angle);
		gravity.y = Constants.gravityFactor * PApplet.cos(angle);
	}

	/**
	 * Determine if a circular body collides (overlaps)
	 * with another one.
	 * @param b1 Body to be tested for collision
	 * @return True if colliding
	 */
	public boolean inCollision(Body b1)
	{
		for (int i = 0; i < this.bodies.size(); i++)
		{
			if (b1.index != i)
			{
				Body b2 = bodies.get(i);
				if (b1.separation(b2) < b1.minSeparation(b2))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Apply forces and move bodies.
	 * @param dt Time interval
	 */
	private void accelerate(float dt)
	{
		for (int i = 0; i < this.bodies.size(); i++)
		{
			Body b = bodies.get(i);
			b.addForce(gravity);
			if(!b.hasCollidedTerrain) {
				b.addForce(wind.getForce());
			}
			b.step(dt, shockFactor);
		}
	}
	
	/**
	 * Modify the position of a cannon body.
	 * @param center New position
	 * @param human Boolean to differentiate between the human and the opponent
	 */
	public void setCannonCollisionPosition(PVector center, boolean human) {
		if(human) {
			cannonCollision1.pos.set(center);
		} else {
			cannonCollision2.pos.set(center);
		}
	}
	
	/**
	 * Determine if a body collides with a cannon (before hitting a mountain).
	 * @param b Body to be tested for collision
	 * @param cannon Body of the cannon potentially colliding
	 * @throws GameException If there is a valid collision.
	 * 		   (either "YOU WON!" or "YOU LOST!")
	 */
	private void cannonCollide(Body b, Body cannon) throws GameException {
		float separation = b.separation(cannon);
		float minSeparation = b.minSeparation(cannon);
		if (!b.hasCollidedTerrain && b.human!=cannon.human && separation < minSeparation) {
			if(cannon.human) {
				throw new GameException("YOU LOST!");
			} else {
				throw new GameException("YOU WON!");
			}
		}
	}

	/**
	 * Collision detection and resolution for between circular bodies.
	 * It uses hard constraints.
	 * @param preservingImpulse True to preserve the impulse
	 * @param preservingMomentum True to preserver the momemtum
	 * @throws GameException If there is a valid collision with a cannon
	 */
	private void bodyCollide(boolean preservingImpulse, boolean preservingMomentum) throws GameException
	{
		int len = bodies.size();
		PVector dn = new PVector();
		PVector dt = new PVector();
		PVector v1 = new PVector();
		PVector v2 = new PVector();
		PVector v1t = new PVector(), v1n = new PVector(), v2t = new PVector(), v2n = new PVector();
		float separation, minSeparation;
		float M, m1, m2;
		PVector proj1 = new PVector();
		PVector proj2 = new PVector();
		for (int i = 0; i < len; i++) {
			Body b1 = bodies.get(i);
			if (useCannonCollision) {
				cannonCollide(b1, cannonCollision1);
				cannonCollide(b1, cannonCollision2);
			}
			for (int j = i + 1; j < len; j++) {
				Body b2 = bodies.get(j);
				//TODO: Should use distance squared for faster detection..
				separation = b1.separation(b2);
				minSeparation = b1.minSeparation(b2);
				dn = PVector.sub(b1.pos, b2.pos);
				if (separation < minSeparation && separation != 0) {
					m1 = b1.mass;
					m2 = b2.mass;
					M = m1 + m2;
					// Hard constraint: resolve the body overlap conflict
					float conflictingLength = (minSeparation - separation) / separation;
					b1.pos.add(PVector.mult(dn, (preservingMomentum)? m2/M *conflictingLength : 0.5f * conflictingLength));
					b2.pos.sub(PVector.mult(dn, (preservingMomentum)? m1/M *conflictingLength : 0.5f * conflictingLength));				
					if (preservingImpulse) {
						v1 = PVector.sub(b1.pos, b1.ppos);
						v2 = PVector.sub(b2.pos, b2.ppos);
						if(preservingMomentum) {
							//http://en.wikipedia.org/wiki/Elastic_collision#Two-dimensional_C.23_example
							dn.normalize();
							dt.set(dn.y,-dn.x,0);
							v1n.set(dn); v2n.set(dn); v1t.set(dt); v2t.set(dt);
							v1n.mult(Constants.damping*v1.dot(dn));
							v1t.mult(Constants.damping*v1.dot(dt));
							v2n.mult(Constants.damping*v2.dot(dn));
							v2t.mult(Constants.damping*v2.dot(dt));
							// I'm using ' - ' here, error in wikipedia? It cannot only be positive...
							// TODO: There could be an error here.
							v1.set(PVector.add(v1t, PVector.mult(dn, 2*m2/M*v2n.mag() - (m1-m2)/M*v1n.mag())));
							v2.set(PVector.sub(v2t, PVector.mult(dn, 2*m1/M*v1n.mag() - (m2-m1)/M*v2n.mag())));
						} else {
							dn.normalize(proj1);
							dn.normalize(proj2);
							proj1.mult(Constants.damping*proj1.dot(v1));
							proj2.mult(Constants.damping*proj2.dot(v2));
							v1.add(PVector.sub(proj2, proj1));
							v2.add(PVector.sub(proj1, proj2));
						}
						b1.ppos.set(PVector.sub(b1.pos, v1));
						b2.ppos.set(PVector.sub(b2.pos, v2));
					}
				}
			}
		}
	}
	
	/**
	 * Hard constraints for collisions on the edges of the scene.
	 * @param preservingImpulse True to preserve the impulse
	 */
	void borderCollide(boolean preservingImpulse){
	    PVector vel;
	    for(int i=0; i<this.bodies.size(); i++)
	    {
	      Body b = bodies.get(i);
	      vel = PVector.sub(b.ppos, b.pos);
	      vel.mult(Constants.damping);
	      if(b.pos.x - b.rad < 0) {
	        b.pos.x = b.rad;
	        if(preservingImpulse) b.ppos.x = b.pos.x - vel.x;
	      } else if(b.pos.x + b.rad > p5.width) {
	        b.pos.x = p5.width-b.rad;
	        if(preservingImpulse) b.ppos.x = b.pos.x - vel.x;
	      }
	      if(b.pos.y - b.rad < 0) {
	        b.pos.y =  b.rad;
	        if(preservingImpulse) b.ppos.y = b.pos.y - vel.y;
	      } else if(b.pos.y + b.rad > p5.height) {
	        b.pos.y = p5.height-b.rad;
	        if(preservingImpulse) b.ppos.y = b.pos.y - vel.y;
	      }
	    }
	  }

	/**
	 * Collision detection and resolution for between circular bodies and the terrain.
	 * @param terrain Terrain constraint
	 * @param preservingImpulse True to preserve the impulse
	 */
	private void terrainCollide(Terrain terrain, boolean preservingImpulse)
	{
		int len = bodies.size();
		float separation, height;
		/* Instead of using a quadtree (which would indeed be more efficient)
		 * we simply use two terrain vertices index for more limiting
		 * the search to 1/4 of the terrain length: in between these two vertices.
		 * It is fast enough for this application.
		 */
		int low, high;
		
		PVector distv = new PVector();
		PVector pathv;
		PVector radv = new PVector();
		for(int nb=0; nb<2; ++nb) {
			for (int i = 0; i < len; i++) {
				boolean collided = false;
				Body b = bodies.get(i);
				int[] indices = terrain.getBoundingIndices(b.pos);
				low = indices[0]; high = indices[1];
				if(!b.hasCollidedTerrain) {
					//Obtain the path/line traced by the projectile body
					//with the radius added to it.
					pathv = PVector.sub(b.pos, b.ppos);
					radv.set(pathv);
					radv.normalize();
					radv.mult(b.rad);
					// I think this extra length to the path somehow reinforces
					// the "full-stop" behavior which is required in this assignment
					pathv.mult(1.22f);
					for(int j=low; j<high; ++j) {
						/* If the path intersects the terrain, apply a full
						 * stop to the body motion.
						 */
						PVector intersection = intersect(PVector.add(b.pos, radv),
								pathv,
								terrain.heightmap[j],
								PVector.sub(terrain.heightmap[j+1], terrain.heightmap[j]));
						if(intersection != null) {
							b.pos = PVector.sub(intersection, radv);
							b.ppos.set(b.pos);
							collided = true;
							break;
						}
					}
				}
				for(int j=low; j<high; ++j) {
					separation = b.separation(terrain.heightmap[j]);
					distv = PVector.sub(b.pos, terrain.heightmap[j]);
					if ((separation < b.rad && separation != 0))
					{
						/* Hard constraint with a simple overlap between
						 * the circle and the terrain vertices */
						float conflictingLength = (b.rad - separation) / separation;
						b.pos.add(PVector.mult(distv, conflictingLength));
						if(collided) {
							b.ppos.set(b.pos); //full-stop
						}
					}
					/* Additional safety constrain based on the body's height.
					 * Works fine without this but this additional constraint
					 * might help if there are many projectiles stacked on each
					 * other, or if the space between the terrain vertices is
					 * very wide. The resulting behavior is not perfect however. */
					if( j>0 && b.pos.x > terrain.heightmap[j-1].x && b.pos.x < terrain.heightmap[j].x) {
						height = PApplet.lerp(terrain.heightmap[j-1].y,
								terrain.heightmap[j].y,
								(b.pos.x-terrain.heightmap[j-1].x)/(terrain.terrainSegmentWidth));
						if(b.pos.y + b.rad > height) {
							b.pos.y = height-b.rad;
							b.ppos.y = b.pos.y;
						}
					} else if (b.pos.x > terrain.heightmap[j].x && b.pos.x < terrain.heightmap[j+1].x){
						height = PApplet.lerp(terrain.heightmap[j].y,
								terrain.heightmap[j+1].y,
								(b.pos.x-terrain.heightmap[j].x)/(terrain.terrainSegmentWidth));
						if(b.pos.y + b.rad > height) {
							b.pos.add(PVector.mult(terrain.normals[j], (b.pos.y + b.rad - height)));
							b.pos.y = height-b.rad;
							b.ppos.y = b.pos.y;
						}
					}
				}
				if(collided) {
					b.hasCollidedTerrain = true;
				}
			}
		}
	}

	private float cross2d( PVector v1, PVector v2) {
		return v1.x * v2.y - v1.y * v2.x;
	}

	/**
	 * Intersection between two line segments.
	 * @param point1 Initial point of the first segment.
	 * @param vec1 Vector of the first segment.
	 * @param point2 Initial point of the second segment.
	 * @param vec2 Vector of the second segment
	 * @return PVector of the intersection point (otherwise null)
	 */
	private PVector intersect(PVector point1, PVector vec1, PVector point2, PVector vec2) {
		float scalev1 = cross2d(PVector.sub(point2, point1), vec2)/cross2d(vec1, vec2);
		float scalev2 = cross2d(PVector.sub(point1, point2), vec1)/cross2d(vec2, vec1);
		// Parallel
		if (cross2d(vec1, vec2)==0) {
			return null;
		}
		if (0 < scalev1 && scalev1 < 1 && 0<scalev2 && scalev2 <1) {
			return PVector.add(point1,PVector.mult(vec1, scalev1));
		} 
		else {
			return null;
		}
	}

	/**
	 * Preserve bodies' inertia.
	 */
	private void inertia()
	{
		for (int i = 0; i < this.bodies.size(); i++)
		{
			bodies.get(i).preserveInertia();
		}
	}

	/**
	 * Testing: Shock the system to test the stability.
	 */
	public void shock()
	{
		shockFactor = 1.0f;
	}

	/**
	 * Advance the simulation by one step
	 * @param terrain Terrain for collision
	 * @throws GameException If there is a valid collision between a body and a cannon
	 */
	public void step(Terrain terrain) throws GameException
	{
		wind.step();
		int steps = 1;
		float dt = (float) steps / 2;
		for (int i = 0; i < steps; i++)
		{
			accelerate(dt);
			bodyCollide(false, true);
			if(useBorderConstraint)
				borderCollide(false);
			terrainCollide(terrain, false);
			inertia();
			bodyCollide(true, true);
			if(useBorderConstraint)
				borderCollide(true);
		}
		shockFactor -= 0.01;
		shockFactor = PApplet.max(shockFactor, 0);
		postStep(terrain.horizon);

		zoff += zIncrement;
		for(int i=0; i<nbDustParticles; ++i) {
			dustParticles[i].step(zoff, wind.getForce());
		}
	}
	
	/**
	 * Post-step cleanup. Remove bodies outside the canvas.
	 * @param horizon Water level.
	 */
	private void postStep(int horizon)
	{
		if (!useBorderConstraint) {
			for (int i = this.bodies.size()-1; i >= 0; --i) {
				Body b = bodies.get(i);
				if ( b.pos.x + b.rad < 0 || b.pos.x - b.rad > p5.width 
						|| b.pos.y - b.rad > p5.height || b.pos.y - b.rad > horizon) {
					bodies.remove(i);
				}
			}
		}
	}

	void draw() {
		p5.noStroke();
		for(int i=0; i<nbDustParticles; ++i) {
			dustParticles[i].draw();
		}
		wind.draw();
		for (int i = 0; i < this.bodies.size(); i++) {
			bodies.get(i).draw(shockFactor);
		}
//		p5.fill(0,0,255);
//		cannonCollision1.draw(shockFactor);
//		cannonCollision2.draw(shockFactor);
	}
}
