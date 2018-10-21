package com.mygdx.vrstream;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.IIOException;

public class MyVrStreamServer extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img;

	Server server;
	SimplePose pose;

	Environment environment;
	ModelBatch modelBatch;
	PerspectiveCamera cam;
	Array<ModelInstance> instances;
	ModelInstance box;
	Model model;
	FrameBuffer frameBuffer;
	Texture tex;
	Pixmap pix;
	byte[] pixelData;
	@Override
	public void create () {
		batch = new SpriteBatch();
		img = new Texture("badlogic.jpg");

		//KRYONET
		//...........................................................
		float[] trans = {0,0,0};
		float[] rot = {0,0,0,0};
		pose = new SimplePose(trans,rot);

		server = new Server(1881,17000);
		server.start();
		try{
			server.bind(5444, 5777);
		}catch (IOException e){}

		Kryo kryo = server.getKryo();
		kryo.register(SimplePose.class);
		kryo.register(float[].class);
		kryo.register(byte[].class);

		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof SimplePose) {
					SimplePose request = (SimplePose)object;
					pose = request;

				}
			}
		});

		//GRAPHICS
		//...........................................................
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
		modelBatch = new ModelBatch();
		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(3f, 2f, 0);
		cam.lookAt(0,0,0);
		cam.near = 1f;
		cam.far = 300f;
		cam.update();

		ModelBuilder modelBuilder = new ModelBuilder();
		instances = new Array<ModelInstance>();
		model = modelBuilder.createBox(0.5f, 0.5f, 0.5f, new Material(ColorAttribute.createDiffuse(Color.GREEN)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		box = new ModelInstance(model,0,0,0);
		instances.add(box);

		frameBuffer = new FrameBuffer(Pixmap.Format.RGB888,Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),false);
		pix = new Pixmap(64,64, Pixmap.Format.RGB888);
		pix.setColor(10,0,0,0);
		ByteBuffer buff = pix.getPixels();
	}

	@Override
	public void render () {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		box.transform.setToTranslation(pose.translation[0],pose.translation[1],pose.translation[2]);

		modelBatch.begin(cam);
		modelBatch.render(box, environment);
		modelBatch.end();

		//byte[] pixelData = ScreenUtils.getFrameBufferPixels(true);
		server.sendToAllTCP(pixelData);
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
		try{server.dispose();}catch (IOException e){}
		modelBatch.dispose();
		model.dispose();
		instances.clear();

	}
}
