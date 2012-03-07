import processing.core.PApplet;
import processing.core.PVector;

public class Terrain
{
	/*
	 * IMPORTANT REMINDER: The y-coordinate increases as it goes down the screen.
	 */
	/** The parent PApplet that we will render ourselves onto. */
	private PApplet p5;
	/** Water level. */
	final int horizon;
	/** Height of the noise */
	final int noiseHeight;
	/** Array of terrain PVector vertices */
	PVector heightmap[];
	/** Array of terrain PVector normals (perpendicular to the terrain) */
	PVector normals[];
	/** Width of a single terrain segment. */
	final int terrainSegmentWidth;
	/** Number of terrain segments over the width of the canvas. */
	final int nbSegments;
	/** Inidices of important terrain points */
	public int summit1Index, summit2Index, seaFloorIndex;
	
	Terrain(PApplet p, int horizon, int noiseHeight)
	{
		this.p5 = p;
		p5.noiseDetail(4, 0.48f);
		this.horizon = horizon;
		this.noiseHeight = noiseHeight;
		this.terrainSegmentWidth = 4;
		this.nbSegments = p5.width/terrainSegmentWidth+1;
		heightmap = new PVector[nbSegments];
		normals = new PVector[nbSegments];
		addMountains();
		computeNormals();
	}
	
	/** Add the general mountain shape to the heightmap using simple
	 * linear interpolation between the edges/summits/seafloor.
	 */
	private void addMountains()
	{
		PVector seaFloor = new PVector(p5.width/2,horizon+175);
		PVector summit1 = new PVector();
		PVector summit2 = new PVector();
		summit1.x = (int) p5.random(0.10f*p5.width, 0.20f*p5.width);
		summit1.y = horizon-(int) p5.random(150, 400);
		summit2.x = (int) p5.random(0.80f*p5.width, 0.90f*p5.width);
		summit2.y = horizon-(int) p5.random(150, 400);
		for(int i=0; i<nbSegments; ++i) {
			int x = i*terrainSegmentWidth;
			if(x < summit1.x) {
				heightmap[i] = moutainLinearInterpolation(new PVector(0, horizon), summit1, x);
				summit1Index = i+1;
			} else if (x < seaFloor.x) {
				heightmap[i] = moutainLinearInterpolation(summit1, seaFloor, x);
				seaFloorIndex = i+1;
			} else if (x < summit2.x) {
				heightmap[i] = moutainLinearInterpolation(seaFloor, summit2, x);
				summit2Index = i+1;
			} else {
				heightmap[i] = moutainLinearInterpolation(summit2, new PVector(p5.width, horizon), x);
			}
		}
		addNoise();
	}
	/**
	 * Return the interpolated moutain point based on the current x-coord.
	 * @param start Starting interpolation point
	 * @param end Ending interpolation point
	 * @param x Current x-coord
	 * @return Interpolated point as a PVector
	 */
	private PVector moutainLinearInterpolation(PVector start, PVector end, int x)
	{
		int width = (int) (end.x - start.x);
		float inter = (float)(x - start.x)/(float)(width);
		return new PVector(x, start.y+(int)(inter*(end.y - start.y)));
	}
	
	/** Add the perlin noise to the heightmap */
	private void addNoise()
	{
		for(int i=0; i<nbSegments; ++i) {
			heightmap[i].y += noiseHeight*(p5.noise(0.06f*i)-0.5f);
		}
	}
	
	/** 
	 * Compute the terrain normals based on adjacent vertices 
	 **/
	private void computeNormals()
	{
		PVector normal;
		for(int i=1; i<nbSegments-1; ++i) {
			normal = PVector.sub(heightmap[i+1], heightmap[i-1]);
			float temp = normal.x;
			normal.x = normal.y;
			normal.y = -temp;
			normal.normalize();
			normals[i]= normal.get();
		}
		normals[0] = normals[1].get();
		normals[heightmap.length - 1] = normals[heightmap.length - 2].get();
	}
	
	/**
	 * Obtain the first cannon position (2/3 up the first mountain).
	 * @return PVector position
	 */
	public PVector getFirstCannonPosition()
	{
		int index = summit1Index;
		while(heightmap[index].y < horizon) {
			++index;
		}
		return heightmap[summit1Index+(index-summit1Index)/3].get();
	}
	
	/**
	 * Obtain the second cannon position (2/3 up the first mountain).
	 * @return PVector position
	 */
	public PVector getSecondCannonPosition()
	{
		int index = summit2Index;
		while(heightmap[index].y < horizon) {
			--index;
		}
		return heightmap[summit2Index-(summit2Index-index)/3].get();
	}
	
	/**
	 * Draw the water line as a simple blue quad.
	 */
	public void drawWater()
	{
		p5.fill(33, 44, 180);
		p5.quad(0, p5.height, 0, horizon, p5.width, horizon, p5.width, p5.height);
	}
	
	/**
	 * Draw the terrain normals as green line segments.
	 */
	public void drawNormals()
	{
		p5.strokeWeight(1.0f);
		p5.stroke(0,255,0);
		float length = 15.0f;
		for(int i=0; i<nbSegments; ++i) {
			p5.line(heightmap[i].x, heightmap[i].y,
					heightmap[i].x+length*normals[i].x,
					heightmap[i].y+length*normals[i].y);
		}
		p5.noStroke();
	}
	
	/**
	 * Get heightmap indices enclosing the current
	 * position (x-coord). This accelerates the collision detection.
	 * @param pos Current position
	 * @return Array of two int indices
	 */
	public int[] getBoundingIndices(PVector pos)
	{
		int[] pair = new int[2];
		if (pos.x < heightmap[summit1Index].x) {
			pair[0] = 0;
			pair[1] = summit1Index;
			return pair;
		} else if (pos.x < heightmap[seaFloorIndex].x) {
			pair[0] = summit1Index;
			pair[1] = summit2Index;
			return pair;
		} else if (pos.x < heightmap[summit2Index].x) {
			pair[0] = seaFloorIndex;
			pair[1] = summit2Index;
			return pair;
		} else {
			pair[0] = summit2Index;
			pair[1] = heightmap.length-1;
			return pair;
		}
	}
	
	public void draw() {
		p5.noStroke();
		p5.fill(177, 162, 150);
		int h = p5.height;
		for(int i=1; i<heightmap.length; ++i) {
			p5.quad(heightmap[i-1].x, heightmap[i-1].y, heightmap[i].x, heightmap[i].y, heightmap[i].x, h, heightmap[i-1].x, h);
		}
	}
}
