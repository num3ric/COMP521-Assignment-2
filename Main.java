import processing.core.*;

@SuppressWarnings("serial")
public class Main extends PApplet
{
	enum GameState {PLAY, WAIT};
	GameState gameState;
	System system;
	Terrain terrain;
	Cannon cannon1, cannon2;
	float angle;
	int bgColor;
	RandomOpponent opponent;
	PFont font;
	String gameException;
	boolean fillDemo;
	boolean recording;

	public void setup()
	{
		size(1104, 600);
//		size(640, 480);
		frameRate(60);
		fill(255);
		noStroke();
		rectMode(CENTER);
		smooth();
		font = createFont("Arial Bold",48);
		system = new System(this);
		reset();
		opponent = new RandomOpponent(this, cannon2, system);
		opponent.enable();
		background(bgColor);
		gameState = GameState.PLAY;
	}
	
	/** Reset the state of the terrain and system */
	public void reset() {
		bgColor = Constants.somecolor();
		if(system != null)
			system.reset();
		terrain = new Terrain(this, (int) (0.85f * height), (int) (0.20f * height));
		cannon1 = new Cannon(this, terrain.getFirstCannonPosition(), true);
		cannon2 = new Cannon(this, terrain.getSecondCannonPosition(), false);
		if(system != null)
			system.setCannonCollisions(cannon1.getCollisionCircleCenter(),
					0.75f*cannon1.length/2, //smaller for more difficulty
					cannon2.getCollisionCircleCenter(),
					0.75f*cannon2.length/2); //smaller for more difficulty
		if(opponent != null)
			opponent.reset(cannon2);
		fillDemo = false;
	}
	
	/** MAIN GAME LOOP */
	public void draw()
	{
		if(gameException != null) {
			noLoop();
			background(0, 50);
			textFont(font,30);
			fill(255);
			textAlign(CENTER);
			text(gameException,width/2,height/2);
			textFont(font,12);
			text("Click to continue.",width/2,height/2+20);
			textAlign(LEFT);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			gameException = null;
			return;
		}
		boolean win = false;
		try
		{
			system.step(terrain);
		}
		catch (GameException e)
		{
			gameException = e.getMessage();
			if (gameException.contains("WON")) {
				win = true;
			}
			gameState = GameState.WAIT;
		}
		controlCannon();
		background(bgColor);
		system.draw();
		if(!fillDemo)
			terrain.drawWater();
		terrain.draw();
		if(gameException != null) {
			if(win) {
				cannon1.draw();
				cannon2.drawBroken();
			} else {
				cannon1.drawBroken();
				cannon2.draw();
			}
		} else {
			cannon1.draw();
			cannon2.draw();
		}
		opponent.step();
		textFont(font,10);
		fill(0);
		text("fps: "+frameRate,20,20);
		text("Press r to reset landscape",20,35);
		text("Press and hold spacebar to shoot",20,50);
		text("Use arrows to change cannon angle",20,65);
		text("Demo: Press k to commit suicide",20,80);
		text("Demo: Press m to test momemtum",20,95);
		text("Stability test: Press f to fill with balls",20,110);
//		if(recording) {
//			saveFrame("a2-####.png"); 
//		}
	}
	
	void controlCannon() {
		//TODO: We cannot charge the cannon and change the angle at the same time :(
		if (gameState == GameState.PLAY) {
			if(keyPressed) {
				if(key == ' ')
					cannon1.increaseForce(0.02f);
				if (keyCode == UP) {
					cannon1.increaseAngle(0.02f);
					system.setCannonCollisionPosition(cannon1.getCollisionCircleCenter(), true);
				}
				if (keyCode == DOWN){
					cannon1.decreaseAngle(0.02f);
					system.setCannonCollisionPosition(cannon1.getCollisionCircleCenter(), true);
				} 
			}
		}	
	}
	
	void demoFillSystem() {
		int total = 200+system.getNumberOfBodies();
		system.useBorderConstraint(true);
		system.useCannonCollision(false);
		while(system.getNumberOfBodies() < total) {
			Body b = new Body(this, new PVector(random(0,width), random(0,height)),
					random(0.5f, 1.0f), system.getNumberOfBodies()<total/2);
			system.safeAddBody(b);
		}
		fillDemo = true;
	}
	
	void demoMomentum() {
		Body b1 = new Body(this, new PVector(width/2-50, height/2), 2.0f, true);
		b1.addForce(new PVector(30.0f, 0f));
		system.safeAddBody(b1);
		
		Body b2 = new Body(this, new PVector(width/2+50, height/2+random(5)), 0.6f, false);
		b2.addForce(new PVector(-30.0f, 0f));
		system.safeAddBody(b2);
	}

	public void mousePressed()
	{
//		system.shock();
		if (gameState == GameState.WAIT) {
			gameState = GameState.PLAY;
			reset();
			loop();
		} 
	}

	public void keyPressed()
	{
		if (gameState == GameState.PLAY) {
			if (key == 'r' || key == 'R') {
				reset();
			} else if ( key == 's' || key == 'S') {
				recording = !recording;
			} else if (key == 'k' || key == 'K') {
				PVector above = new PVector(10, -50, 0);
				above.add(cannon1.getPosition());
				Body b = new Body(this, above, 1.0f, false);
				system.addBody(b);
			} else if (key == 'f' || key == 'F') {
				demoFillSystem();
			} else if (key == 'm' || key == 'M') {
				demoMomentum();
			}
		}
	}
	
	public void keyReleased() {
		if (gameState == GameState.PLAY) {
			if (key == ' ') {
				if(cannon1.shoot(system)) {
					//pass
				} else {
					textFont(font,30);
					fill(0);
					text("BLOCKED!",cannon1.getPosition().x, cannon1.getPosition().y);
				}
			}
		}
	}

	public static void main(String args[])
	{
		PApplet.main(new String[] { "Main" });
	}
}