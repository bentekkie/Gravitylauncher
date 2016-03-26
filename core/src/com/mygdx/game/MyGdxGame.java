package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class MyGdxGame extends ApplicationAdapter implements GestureDetector.GestureListener {
	SpriteBatch batch;
	Texture img;
	private final appIcons appicons;
	ArrayList<String> icons;
	ArrayList<ArrayList> texturemap;
	World world;
	OrthographicCamera camera;
	Matrix4 debugMatrix;
	final float PIXELS_TO_METERS = 100f;
	public MyGdxGame(appIcons appicons){
		this.appicons = appicons;
	}
	Vector3 point;
	Body bodyThatWasHit;

	@Override
	public boolean touchDown(float x, float y, int pointer, int button) {
		return false;
	}

	@Override
	public boolean tap(float x, float y, int count, int button) {
		point = new Vector3();
		point.set(Gdx.input.getX(),Gdx.input.getY(),0); // Translate to world coordinates.
		point = camera.unproject(point);
		point.x = point.x/PIXELS_TO_METERS;
		point.y = point.y/PIXELS_TO_METERS;
		Gdx.app.log("hello", point.x + "," + point.y);
		// Ask the world for bodies within the bounding box.
		QueryCallback callback = new QueryCallback() {
			@Override
			public boolean reportFixture (Fixture fixture) {
				if (fixture.testPoint(point.x, point.y)) {
					bodyThatWasHit = fixture.getBody();
					return false;
				} else
					return true;
			}
		};
		bodyThatWasHit = null;
		world.QueryAABB(callback, point.x - 0.001f, point.y - 0.001f, point.x + 0.001f, point.y + 0.001f);

		if(bodyThatWasHit != null) {

			appicons.openApp((String)bodyThatWasHit.getUserData());
			// Do something with the body
		}

		return true;
	}

	@Override
	public boolean longPress(float x, float y) {
		Gdx.app.log("hello","I got here");
		appicons.selectApps();
		return false;
	}

	@Override
	public boolean fling(float velocityX, float velocityY, int button) {
		Gdx.app.log("hello","I got here");

		appicons.selectApps();
		return false;
	}

	@Override
	public boolean pan(float x, float y, float deltaX, float deltaY) {
		return false;
	}

	@Override
	public boolean panStop(float x, float y, int pointer, int button) {
		return false;
	}

	@Override
	public boolean zoom(float initialDistance, float distance) {
		return false;
	}

	@Override
	public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {

		return false;
	}

	public interface appIcons{
		ArrayList<String> getIcons();
		void openApp(String packageName);
		Texture getIcon(String packageName);
		ArrayList<String> selectApps();
		Texture getWallpaper();

	}
	Sprite wallpaper;
	@Override
	public void create () {
		Box2D.init();
		wallpaper = new Sprite(appicons.getWallpaper());
		wallpaper.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		wallpaper.setPosition(-Gdx.graphics.getWidth()/2,-Gdx.graphics.getHeight()/2);
		prevAccelY= Gdx.input.getAccelerometerY();
		prevAccelX= Gdx.input.getAccelerometerX();
		batch = new SpriteBatch();
		icons = appicons.getIcons();
		for(String packageName:icons) {
			Gdx.app.log("hello", packageName);
		}
		createBodies();
		Gdx.input.setInputProcessor(new GestureDetector(this));
	}
	public ArrayList<String> toBeAdded(ArrayList<String> base, ArrayList<String> tba){
		ArrayList<String> temp = new ArrayList<String>();
		for(String packageName:tba){
			if(!base.contains(packageName)) temp.add(packageName);
		}
		return temp;
	}
	float prevAccelX;
	float prevAccelY;
	private void processAccelerometer() {
		float y = Gdx.input.getAccelerometerY();
		float x = Gdx.input.getAccelerometerX();
		world.setGravity(new Vector2(-x*gmult, -y*gmult));//Negative on the x, but not on the Y. Somewhat geocentric view.
		Gdx.app.log("hello",Float.toString(-x*gmult)+","+Float.toString(-y*gmult));
	}
	int gmult = 3;
	@Override
	public void render () {
		world.step(1f / 60f, 6, 2);
		processAccelerometer();
		ArrayList<String> newIcons = appicons.getIcons();
		ArrayList<String> toAdd = toBeAdded(icons,newIcons);
		ArrayList<String> toRemove = toBeAdded(newIcons,icons);
		for(String packageName:toAdd){
			createBody(packageName);
		}
		for(String packageName:toRemove){
			removeBody(packageName);
		}
		for(int x = 0; x < toAdd.size(); x++) icons.add(toAdd.get(x));
		for(int x = 0; x < toRemove.size(); x++) icons.remove(toRemove.get(x));
		camera.update();
		world.step(1f / 60f, 6, 2);
		Iterator it = bodies.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			ArrayList temp = (ArrayList)pair.getValue();
			Sprite sprite = (Sprite)temp.get(0);
			Body body = (Body)temp.get(1);
			sprite.setPosition((body.getPosition().x * PIXELS_TO_METERS) - sprite.getWidth()/2 ,
					(body.getPosition().y * PIXELS_TO_METERS) -sprite.getHeight()/2 );
			sprite.setRotation((float)Math.toDegrees(body.getAngle()));
			 // avoids a ConcurrentModificationException
		}
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.setProjectionMatrix(camera.combined);
		debugMatrix = batch.getProjectionMatrix().cpy().scale(PIXELS_TO_METERS,
				PIXELS_TO_METERS, 0);
		batch.begin();
		wallpaper.draw(batch);
		it = bodies.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			ArrayList temp = (ArrayList)pair.getValue();
			Sprite sprite = (Sprite)temp.get(0);
			batch.draw(sprite, sprite.getX(), sprite.getY(),sprite.getOriginX(),
					sprite.getOriginY(),
					sprite.getWidth(),sprite.getHeight(),sprite.getScaleX(),sprite.
							getScaleY(),sprite.getRotation());
			 // avoids a ConcurrentModificationException
		}

		batch.end();
		//debugRenderer.render(world, debugMatrix);
	}
	HashMap<String, ArrayList> bodies;
	public void createBodies(){
		bodies = new HashMap<String, ArrayList>();
		world = new World(new Vector2(0, -10f),true);
		camera = new OrthographicCamera(Gdx.graphics.getWidth(),Gdx.graphics.
				getHeight());
		BodyDef groundBodyDef =new BodyDef();
		groundBodyDef.position.set(new Vector2(0, -camera.viewportHeight/(2*PIXELS_TO_METERS)));
		Body groundBody = world.createBody(groundBodyDef);
		PolygonShape groundBox = new PolygonShape();
		groundBox.setAsBox(camera.viewportWidth, 50f / PIXELS_TO_METERS);
		groundBody.createFixture(groundBox, 0.0f);
		groundBox.dispose();
		BodyDef skyBodyDef =new BodyDef();
		skyBodyDef.position.set(new Vector2(0, camera.viewportHeight/(2*PIXELS_TO_METERS)));
		Body skyBody = world.createBody(skyBodyDef);
		PolygonShape skyBox = new PolygonShape();
		skyBox.setAsBox(camera.viewportWidth, 50f / PIXELS_TO_METERS);
		skyBody.createFixture(skyBox, 0.0f);
		skyBox.dispose();
		BodyDef lwallBodyDef =new BodyDef();
		lwallBodyDef.position.set(new Vector2(-camera.viewportWidth/(2*PIXELS_TO_METERS),0));
		Body lwallBody = world.createBody(lwallBodyDef);
		PolygonShape lwallBox = new PolygonShape();
		lwallBox.setAsBox(50f / PIXELS_TO_METERS, camera.viewportHeight);
		lwallBody.createFixture(lwallBox, 0.0f);
		lwallBox.dispose();
		BodyDef rwallBodyDef =new BodyDef();
		rwallBodyDef.position.set(new Vector2(camera.viewportWidth/(2*PIXELS_TO_METERS),0));
		Body rwallBody = world.createBody(rwallBodyDef);
		PolygonShape rwallBox = new PolygonShape();
		rwallBox.setAsBox(50f / PIXELS_TO_METERS, camera.viewportHeight);
		rwallBody.createFixture(rwallBox, 0.0f);
		rwallBox.dispose();
		for(final String packageName:icons) {
			createBody(packageName);
		}

		//debugRenderer = new Box2DDebugRenderer();
		//debugRenderer.setDrawAABBs(false);



	}
	public void removeBody(String packageName){
		if(bodies != null) {
			ArrayList temp = bodies.get(packageName);
			if( temp != null) {
				world.destroyBody((Body) temp.get(1));
				bodies.remove(packageName);
			}
		}
	}
	public void createBody(String packageName){
		Sprite s = new Sprite(appicons.getIcon(packageName));
		s.setPosition(0, -s.getHeight() / 2);
		Random rand = new Random();
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyDef.BodyType.DynamicBody;
		bodyDef.position.set((rand.nextFloat() + (s.getX() + s.getWidth() / 2) /
						PIXELS_TO_METERS),
				(rand.nextFloat() + (s.getY() + s.getHeight() / 2) / PIXELS_TO_METERS));

		final Body body = world.createBody(bodyDef);
		body.setUserData(packageName);
		CircleShape shape = new CircleShape();
		shape.setRadius(s.getWidth() / 2 / PIXELS_TO_METERS);
		

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.density = 0.1f;

		body.createFixture(fixtureDef);
		ArrayList temp = new ArrayList();
		temp.add(s);
		temp.add(body);
		bodies.put(packageName, temp);
		shape.dispose();
	}
	@Override
	public void dispose() {
		world.dispose();
	}
}
