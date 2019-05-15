package com.bsp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;

public class GameScreen extends ScreenAdapter implements InputProcessor {

    String vertexShader = "attribute vec4 a_position;    \n" +
            "attribute vec4 a_color;\n" +
            "attribute vec2 a_texCoords;\n" +
            "attribute float a_texIndex;\n" +
            "uniform mat4 u_projTrans;\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "varying float v_texIndex;\n" +
            "void main()                  \n" +
            "{                            \n" +
            "   v_color = a_color; \n" +
            "   v_texCoords = a_texCoords; \n" +
            "   v_texIndex = a_texIndex; \n" +
            "   gl_Position =  u_projTrans * a_position;  \n" +
            "}                            \n";
    String fragmentShader = "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "varying float v_texIndex;\n" +
            "uniform sampler2D u_texture0;\n" +
            "uniform sampler2D u_texture1;\n" +
            "uniform sampler2D u_texture2;\n" +
            "uniform sampler2D u_texture3;\n" +
            "uniform sampler2D u_texture4;\n" +
            "uniform sampler2D u_texture5;\n" +
            "uniform sampler2D u_texture6;\n" +
            "uniform sampler2D u_texture7;\n" +
            "uniform sampler2D u_texture8;\n" +
            "uniform sampler2D u_texture9;\n" +
            "uniform sampler2D u_texture10;\n" +
            "uniform sampler2D u_texture11;\n" +
            "uniform sampler2D u_texture12;\n" +
            "uniform sampler2D u_texture13;\n" +
            "uniform sampler2D u_texture14;\n" +
            "uniform sampler2D u_texture15;\n" +
            "void main()\n" +
            "{\n" +
            "  if (v_texIndex == 0.0) {\n" +
            "    gl_FragColor = texture2D(u_texture0, v_texCoords);\n" +
            "  } else if (v_texIndex == 1.0) {\n" +
            "    gl_FragColor = texture2D(u_texture1, v_texCoords);\n" +
            "  } else if (v_texIndex == 2.0) {\n" +
            "    gl_FragColor = texture2D(u_texture2, v_texCoords);\n" +
            "  } else if (v_texIndex == 3.0) {\n" +
            "    gl_FragColor = texture2D(u_texture3, v_texCoords);\n" +
            "  } else if (v_texIndex == 4.0) {\n" +
            "    gl_FragColor = texture2D(u_texture4, v_texCoords);\n" +
            "  } else if (v_texIndex == 5.0) {\n" +
            "    gl_FragColor = texture2D(u_texture5, v_texCoords);\n" +
            "  } else if (v_texIndex == 6.0) {\n" +
            "    gl_FragColor = texture2D(u_texture6, v_texCoords);\n" +
            "  } else if (v_texIndex == 7.0) {\n" +
            "    gl_FragColor = texture2D(u_texture7, v_texCoords);\n" +
            "  } else if (v_texIndex == 8.0) {\n" +
            "    gl_FragColor = texture2D(u_texture8, v_texCoords);\n" +
            "  } else if (v_texIndex == 9.0) {\n" +
            "    gl_FragColor = texture2D(u_texture10, v_texCoords);\n" +
            "  } else if (v_texIndex == 11.0) {\n" +
            "    gl_FragColor = texture2D(u_texture12, v_texCoords);\n" +
            "  } else if (v_texIndex == 13.0) {\n" +
            "    gl_FragColor = texture2D(u_texture13, v_texCoords);\n" +
            "  } else if (v_texIndex == 14.0) {\n" +
            "    gl_FragColor = texture2D(u_texture14, v_texCoords);\n" +
            "  } else if (v_texIndex == 15.0) {\n" +
            "    gl_FragColor = texture2D(u_texture15, v_texCoords);\n" +
            "  }\n" +
            "}";
    private Map bsp;

    private float rotationSpeed = 0.3f;
    private ShaderProgram shader;
    private PerspectiveCamera camera;
    private int mouseX;
    private int mouseY;
    private boolean moving;
    private float movementSpeed = 2.0f;

    @Override
    public void show() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1.0f;
        camera.far = 9000.0f;

        Gdx.input.setInputProcessor(this);
        Gdx.input.setCursorCatched(true);

        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(vertexShader, fragmentShader);

        bsp = new Map(Gdx.files.internal("assets/test1.bsp"));
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
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glCullFace(GL20.GL_BACK);

        shader.begin();
        shader.setUniformMatrix("u_projTrans", camera.combined);
        bsp.render(shader);
        shader.end();
    }

    private boolean processMouseLook(int screenX, int screenY) {
        int magX = Math.abs(mouseX - screenX);
        int magY = Math.abs(mouseY - screenY);

        if (mouseX > screenX) {
            camera.rotate(Vector3.Y, -1 * magX * rotationSpeed);
            camera.update();
        }

        if (mouseX < screenX) {
            camera.rotate(Vector3.Y, 1 * magX * rotationSpeed);
            camera.update();
        }

        if (mouseY < screenY) {
            if (camera.direction.y < 0.965)
                camera.rotate(camera.direction.cpy().crs(Vector3.Y), 1 * magY * rotationSpeed);
            camera.update();
        }

        if (mouseY > screenY) {
            if (camera.direction.y > -0.965)
                camera.rotate(camera.direction.cpy().crs(Vector3.Y), -1 * magY * rotationSpeed);
            camera.update();
        }

        mouseX = screenX;
        mouseY = screenY;

        return false;
    }
}
