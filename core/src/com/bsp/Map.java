package com.bsp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Map {

    // bsp components
    private Header header;
    private Vertex[] vertices;
    private Face[] faces;
    private TextureInfo[] textureInfos;
    private Meshvert[] meshverts;

    // file reading bits
    private int bufferSize = 1024;
    private BufferedInputStream stream;
    private byte[] buffer = new byte[bufferSize];

    // libgdx mesh
    private Mesh mesh;
    private Texture[] textures;

    public Map(FileHandle file) {
        try {
            load(file);
            generateMesh();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load(FileHandle file) throws IOException {
        stream = file.read(bufferSize);

        Header header = new Header();
        header.magic = readString(4);
        header.version = readInt();

        for (int i = 0; i < 16; i++) {
            Lump lump = new Lump();
            lump.offset = readInt();
            lump.length = readInt();
            header.lump[i] = lump;
        }

        // textures
        stream = file.read(bufferSize);
        stream.skip(header.lump[1].offset);

        textureInfos = new TextureInfo[header.lump[1].length / 72];
        textures = new Texture[textureInfos.length];

        for (int i = 0; i < textureInfos.length; i++) {
            textureInfos[i] = new TextureInfo();
            textureInfos[i].name = readString(64);
            textureInfos[i].flags = readInt();
            textureInfos[i].contents = readInt();

            FileHandle textureFile = Gdx.files.internal("assets/" + textureInfos[i].name + ".jpg");

            if (textureFile.exists()) {
                textures[i] = new Texture(textureFile, true);
                textures[i].setFilter(Texture.TextureFilter.MipMap, Texture.TextureFilter.Nearest);
                textures[i].setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            }
        }

        // vertices
        stream = file.read(bufferSize);
        stream.skip(header.lump[10].offset);

        vertices = new Vertex[header.lump[10].length / 44];

        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = new Vertex();
            // weird allocation here to suit default opengl coordinate system
            vertices[i].position[2] = readFloat();
            vertices[i].position[0] = readFloat();
            vertices[i].position[1] = -readFloat();
            vertices[i].texcoord[0][0] = readFloat();
            vertices[i].texcoord[0][1] = readFloat();
            vertices[i].texcoord[1][0] = readFloat();
            vertices[i].texcoord[1][1] = readFloat();
            vertices[i].normal[0] = readFloat();
            vertices[i].normal[1] = readFloat();
            vertices[i].normal[2] = readFloat();
            vertices[i].color[0] = readByte();
            vertices[i].color[1] = readByte();
            vertices[i].color[2] = readByte();
            vertices[i].color[3] = readByte();
        }

        // meshverts
        stream = file.read(bufferSize);
        stream.skip(header.lump[11].offset);

        meshverts = new Meshvert[header.lump[11].length / 4];

        for (int i = 0; i < meshverts.length; i++) {
            meshverts[i] = new Meshvert();
            meshverts[i].offset = readInt();
        }

        // faces
        stream = file.read(bufferSize);
        stream.skip(header.lump[13].offset);

        faces = new Face[header.lump[13].length / 104];

        for (int i = 0; i < faces.length; i++) {
            faces[i] = new Face();
            faces[i].texture = readInt();
            faces[i].effect = readInt();
            faces[i].type = readInt();
            faces[i].vertex = readInt();
            faces[i].n_vertexes = readInt();
            faces[i].meshvert = readInt();
            faces[i].n_meshverts = readInt();
            faces[i].lm_index = readInt();
            faces[i].lm_start[0] = readInt();
            faces[i].lm_start[1] = readInt();
            faces[i].lm_size[0] = readInt();
            faces[i].lm_size[1] = readInt();
            faces[i].lm_origin[0] = readFloat();
            faces[i].lm_origin[1] = readFloat();
            faces[i].lm_origin[2] = readFloat();
            faces[i].lm_vecs[0][0] = readFloat();
            faces[i].lm_vecs[0][1] = readFloat();
            faces[i].lm_vecs[0][2] = readFloat();
            faces[i].lm_vecs[1][0] = readFloat();
            faces[i].lm_vecs[1][1] = readFloat();
            faces[i].lm_vecs[1][2] = readFloat();
            faces[i].normal[0] = readFloat();
            faces[i].normal[1] = readFloat();
            faces[i].normal[2] = readFloat();
            faces[i].size[0] = readInt();
            faces[i].size[1] = readInt();
        }
    }

    private void generateMesh() {
        int numberOfIndices = 0;

        for (int i = 0; i < faces.length; i++) {
            // polygon type faces
            if (faces[i].type == 1) {
                // each meshvert is a triangle (3 indices)
                numberOfIndices += faces[i].n_meshverts * 3;
            }
        }

        int meshVerticesComponentCount = 7;

        float[] meshVertices = new float[vertices.length * meshVerticesComponentCount];
        short[] meshIndices = new short[numberOfIndices];

        int meshIndex = 0;

        for (int i = 0; i < faces.length; i++) {
            // polygon type faces
            if (faces[i].type == 1) {
                // each meshvert is a triangle (3 indices)
                for (int j = faces[i].meshvert; j < faces[i].meshvert + faces[i].n_meshverts; j++) {
                    int vertexIndex = faces[i].vertex + meshverts[j].offset;
                    meshIndices[meshIndex++] = (short) vertexIndex;
                    meshVertices[vertexIndex * meshVerticesComponentCount + 6] = faces[i].texture;
                }
            }
        }

        for (int i = 0; i < vertices.length; i++) {
            meshVertices[i * meshVerticesComponentCount + 0] = vertices[i].position[0];
            meshVertices[i * meshVerticesComponentCount + 1] = vertices[i].position[1];
            meshVertices[i * meshVerticesComponentCount + 2] = vertices[i].position[2];
            meshVertices[i * meshVerticesComponentCount + 3] = Color.rgba8888(
                    vertices[i].color[0],
                    vertices[i].color[1],
                    vertices[i].color[2],
                    vertices[i].color[3]);
            meshVertices[i * meshVerticesComponentCount + 4] = vertices[i].texcoord[0][0];
            meshVertices[i * meshVerticesComponentCount + 5] = vertices[i].texcoord[0][1];
        }

        mesh = new Mesh(true, meshVertices.length, meshIndices.length,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_color"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoords"),
                new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_texIndex"));
        mesh.setVertices(meshVertices);
        mesh.setIndices(meshIndices);
    }

    public void render(ShaderProgram shader) {
        for (int i = 0; i < textures.length; i++) {
            if (textures[i] != null) {
                textures[i].bind(i);
                shader.setUniformi("u_texture" + i, i);
            }
        }

        mesh.render(shader, GL20.GL_TRIANGLES);
    }

    private String readString(int length) throws IOException {
        stream.read(buffer, 0, length);

        return new String(buffer, "UTF-8").trim();
    }

    private int readInt() throws IOException {
        stream.read(buffer, 0, 4);

        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private float readFloat() throws IOException {
        stream.read(buffer, 0, 4);

        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    private byte readByte() throws IOException {
        stream.read(buffer, 0, 1);

        return buffer[0];
    }

    class TextureInfo {
        String name;
        int flags;
        int contents;
    }

    class Face {
        int texture;
        int effect;
        int type;
        int vertex;
        int n_vertexes;
        int meshvert;
        int n_meshverts;
        int lm_index;
        int[] lm_start = new int[2];
        int[] lm_size = new int[2];
        float[] lm_origin = new float[3];
        float[][] lm_vecs = new float[2][3];
        float[] normal = new float[3];
        int[] size = new int[2];
    }

    class Meshvert {
        int offset;
    }

    class Vertex {
        float[] position = new float[3];
        float[][] texcoord = new float[2][2];
        float[] normal = new float[3];
        byte[] color = new byte[4];
    }

    class Header {
        String magic;
        int version;
        Lump[] lump = new Lump[17];
    }

    class Lump {
        int offset;
        int length;
    }
}
