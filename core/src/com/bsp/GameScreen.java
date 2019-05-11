package com.bsp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;

public class GameScreen extends ScreenAdapter implements InputProcessor {

    Mesh mesh;
    ShaderProgram shader;
    PerspectiveCamera camera;
    String vertexShader = "attribute vec4 a_position;    \n" +
            "uniform mat4 u_projTrans;\n" +
            "void main()                  \n" +
            "{                            \n" +
            "   gl_Position =  u_projTrans * a_position;  \n" +
            "}                            \n";
    String fragmentShader = "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "void main()                                  \n" +
            "{                                            \n" +
            "  gl_FragColor = vec4(1,1,1,1);\n" +
            "}";
    private int mouseX;
    private int mouseY;
    private boolean moving;
    private float rotationSpeed = 0.3f;
    private float movementSpeed = 0.3f;

    @Override
    public void show() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.005f;
        camera.far = 300;

        Gdx.input.setInputProcessor(this);
        Gdx.input.setCursorCatched(true);

        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(vertexShader, fragmentShader);

        BspMap map = new BspMap(Gdx.files.internal("assets/q2dm1.bsp"));

        float[] vertices = new float[map.vertices.length * 3];
        short[] indices = new short[map.edges.length * 2];

        for (int i = 0; i < map.vertices.length; i++) {
            // scaling down by 1/10th here so it fits in the camera viewport
            vertices[i * 3 + 0] = map.vertices[i].x / 10;
            vertices[i * 3 + 1] = map.vertices[i].y / 10;
            vertices[i * 3 + 2] = map.vertices[i].z / 10;
        }

        for (int i = 0; i < map.edges.length; i++) {
            indices[i * 2 + 0] = map.edges[i].index1;
            indices[i * 2 + 1] = map.edges[i].index2;
        }

        mesh = new Mesh(true, vertices.length, indices.length,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
    }

    @Override
    public void render(float delta) {
        if (moving) {
            camera.position.add(camera.direction.x * movementSpeed,
                    camera.direction.y * movementSpeed,
                    camera.direction.z * movementSpeed);
            camera.update();
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        shader.begin();
        shader.setUniformMatrix("u_projTrans", camera.combined);
        mesh.render(shader, GL20.GL_LINES);
        shader.end();
    }

    private boolean processMouseLook(int screenX, int screenY) {
        int magX = Math.abs(mouseX - screenX);
        int magY = Math.abs(mouseY - screenY);

        if (mouseX > screenX) {
            camera.rotate(Vector3.Y, 1 * magX * rotationSpeed);
            camera.update();
        }

        if (mouseX < screenX) {
            camera.rotate(Vector3.Y, -1 * magX * rotationSpeed);
            camera.update();
        }

        if (mouseY < screenY) {
            if (camera.direction.y > -0.965)
                camera.rotate(camera.direction.cpy().crs(Vector3.Y), -1 * magY * rotationSpeed);
            camera.update();
        }

        if (mouseY > screenY) {
            if (camera.direction.y < 0.965)
                camera.rotate(camera.direction.cpy().crs(Vector3.Y), 1 * magY * rotationSpeed);
            camera.update();
        }

        mouseX = screenX;
        mouseY = screenY;

        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            System.exit(0);
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        moving = true;

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        moving = false;

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return processMouseLook(screenX, screenY);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return processMouseLook(screenX, screenY);
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
