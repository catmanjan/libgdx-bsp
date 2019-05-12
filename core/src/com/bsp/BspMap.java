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

public class BspMap {

    // bsp components
    private BspHeader header;
    private BspVertex[] vertices;
    private BspFace[] faces;
    private int[] faceEdges;
    private BspEdge[] edges;
    private BspPlane[] planes;
    private BspTextureInformation[] textures;

    // file reading bits
    private int bufferSize = 1024;
    private BufferedInputStream stream;
    private byte[] buffer = new byte[bufferSize];

    // libgdx mesh
    private Mesh mesh;
    private Texture texture;

    public BspMap(FileHandle file) {
        try {
            load(file);
            generateMesh();
            texture = new Texture(Gdx.files.internal("assets/grnx2_9.jpg"), true);
            texture.setFilter(Texture.TextureFilter.MipMap, Texture.TextureFilter.Nearest);
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load(FileHandle file) throws IOException {
        byte[] bytes = file.readBytes();

        stream = file.read(bufferSize);

        BspHeader header = new BspHeader();
        header.magic = readNextString(4);
        header.version = readNextInt();

        for (int i = 0; i < 19; i++) {
            BspLump lump = new BspLump();
            lump.offset = readNextInt();
            lump.length = readNextInt();
            header.lump[i] = lump;
        }

        // vertices
        stream = file.read(bufferSize);
        stream.skip(header.lump[2].offset);

        vertices = new BspVertex[header.lump[2].length / 12];

        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = new BspVertex();
            vertices[i].z = readNextFloat();
            vertices[i].x = readNextFloat();
            vertices[i].y = -readNextFloat();
        }

        // faces
        stream = file.read(bufferSize);
        stream.skip(header.lump[6].offset);

        faces = new BspFace[header.lump[6].length / 20];

        for (int i = 0; i < faces.length; i++) {
            faces[i] = new BspFace();
            faces[i].plane = readNextShort();
            faces[i].planeSide = readNextShort();
            faces[i].firstEdge = readNextInt();
            faces[i].numEdges = readNextShort();
            faces[i].textureInfo = readNextShort();
            faces[i].lightmapStyles[0] = readNextByte();
            faces[i].lightmapStyles[1] = readNextByte();
            faces[i].lightmapStyles[2] = readNextByte();
            faces[i].lightmapStyles[3] = readNextByte();
            faces[i].lightmapOffset = readNextInt();
        }

        // edges
        stream = file.read(bufferSize);
        stream.skip(header.lump[11].offset);

        edges = new BspEdge[header.lump[11].length / 4];

        for (int i = 0; i < edges.length; i++) {
            edges[i] = new BspEdge();
            edges[i].index1 = readNextShort();
            edges[i].index2 = readNextShort();
        }

        // faces edges
        stream = file.read(bufferSize);
        stream.skip(header.lump[12].offset);

        faceEdges = new int[header.lump[12].length / 4];

        for (int i = 0; i < faceEdges.length; i++) {
            faceEdges[i] = readNextInt();
        }

        // planes
        stream = file.read(bufferSize);
        stream.skip(header.lump[1].offset);

        planes = new BspPlane[header.lump[1].length / 20];

        for (int i = 0; i < planes.length; i++) {
            planes[i] = new BspPlane();
            planes[i].normal = new BspPoint3f();
            planes[i].normal.x = readNextFloat();
            planes[i].normal.y = readNextFloat();
            planes[i].normal.z = readNextFloat();
            planes[i].distance = readNextFloat();
            planes[i].type = readNextInt();
        }

        // textures
        stream = file.read(bufferSize);
        stream.skip(header.lump[5].offset);

        textures = new BspTextureInformation[header.lump[5].length / 76];

        for (int i = 0; i < textures.length; i++) {
            textures[i] = new BspTextureInformation();
            textures[i].uAxis = new BspPoint3f();
            textures[i].uAxis.x = readNextFloat();
            textures[i].uAxis.y = readNextFloat();
            textures[i].uAxis.z = readNextFloat();
            textures[i].uOffset = readNextFloat();
            textures[i].vAxis = new BspPoint3f();
            textures[i].vAxis.x = readNextFloat();
            textures[i].vAxis.y = readNextFloat();
            textures[i].vAxis.z = readNextFloat();
            textures[i].vOffset = readNextFloat();
            textures[i].flags = readNextInt();
            textures[i].value = readNextInt();
            textures[i].textureName = readNextString(32);
            textures[i].nextTextureInformation = readNextInt();
        }
    }

    private void generateMesh() {
        int numberOfIndices = 0;

        for (int i = 0; i < faces.length; i++) {
            numberOfIndices += faces[i].numEdges * 2;
        }

        float[] meshVertices = new float[vertices.length * 6];
        short[] meshIndices = new short[numberOfIndices];

        for (int i = 0; i < vertices.length; i++) {
            meshVertices[i * 6 + 0] = vertices[i].x;
            meshVertices[i * 6 + 1] = vertices[i].y;
            meshVertices[i * 6 + 2] = vertices[i].z;
            meshVertices[i * 6 + 3] = Color.argb8888(Color.WHITE);
            meshVertices[i * 6 + 4] = 0;
            meshVertices[i * 6 + 5] = 0;
        }

        int idx = 0;

        float texs = 64;

        for (int i = 0; i < faces.length; i++) {
            faces[i].indicesOffset = idx;

            BspTextureInformation texture = textures[faces[i].textureInfo];

            for (int j = faces[i].firstEdge; j < faces[i].firstEdge + faces[i].numEdges; j++) {
                if (faceEdges[j] > 0) {
                    meshIndices[idx++] = edges[faceEdges[j]].index1;
                    meshIndices[idx++] = edges[faceEdges[j]].index2;

                    // gross dotproducts
                    meshVertices[edges[faceEdges[j]].index1 * 6 + 4] =
                            meshVertices[edges[faceEdges[j]].index1 * 6 + 0] * texture.uAxis.x +
                                    meshVertices[edges[faceEdges[j]].index1 * 6 + 1] * texture.uAxis.y +
                                    meshVertices[edges[faceEdges[j]].index1 * 6 + 2] * texture.uAxis.z +
                                    texture.uOffset;
                    meshVertices[edges[faceEdges[j]].index1 * 6 + 4] /= texs;

                    meshVertices[edges[faceEdges[j]].index1 * 6 + 5] =
                            meshVertices[edges[faceEdges[j]].index1 * 6 + 0] * texture.vAxis.x +
                                    meshVertices[edges[faceEdges[j]].index1 * 6 + 1] * texture.vAxis.y +
                                    meshVertices[edges[faceEdges[j]].index1 * 6 + 2] * texture.vAxis.z +
                                    texture.vOffset;
                    meshVertices[edges[faceEdges[j]].index1 * 6 + 5] /= texs;

                    meshVertices[edges[faceEdges[j]].index2 * 6 + 4] =
                            meshVertices[edges[faceEdges[j]].index2 * 6 + 0] * texture.uAxis.x +
                                    meshVertices[edges[faceEdges[j]].index2 * 6 + 1] * texture.uAxis.y +
                                    meshVertices[edges[faceEdges[j]].index2 * 6 + 2] * texture.uAxis.z +
                                    texture.uOffset;
                    meshVertices[edges[faceEdges[j]].index2 * 6 + 4] /= texs;

                    meshVertices[edges[faceEdges[j]].index2 * 6 + 5] =
                            meshVertices[edges[faceEdges[j]].index2 * 6 + 0] * texture.vAxis.x +
                                    meshVertices[edges[faceEdges[j]].index2 * 6 + 1] * texture.vAxis.y +
                                    meshVertices[edges[faceEdges[j]].index2 * 6 + 2] * texture.vAxis.z +
                                    texture.vOffset;
                    meshVertices[edges[faceEdges[j]].index2 * 6 + 5] /= texs;
                } else {
                    meshIndices[idx++] = edges[-faceEdges[j]].index2;
                    meshIndices[idx++] = edges[-faceEdges[j]].index1;

                    meshVertices[edges[-faceEdges[j]].index2 * 6 + 4] =
                            meshVertices[edges[-faceEdges[j]].index2 * 6 + 0] * texture.uAxis.x +
                                    meshVertices[edges[-faceEdges[j]].index2 * 6 + 1] * texture.uAxis.y +
                                    meshVertices[edges[-faceEdges[j]].index2 * 6 + 2] * texture.uAxis.z +
                                    texture.uOffset;
                    meshVertices[edges[-faceEdges[j]].index2 * 6 + 4] /= texs;

                    meshVertices[edges[-faceEdges[j]].index2 * 6 + 5] =
                            meshVertices[edges[-faceEdges[j]].index2 * 6 + 0] * texture.vAxis.x +
                                    meshVertices[edges[-faceEdges[j]].index2 * 6 + 1] * texture.vAxis.y +
                                    meshVertices[edges[-faceEdges[j]].index2 * 6 + 2] * texture.vAxis.z +
                                    texture.vOffset;
                    meshVertices[edges[-faceEdges[j]].index2 * 6 + 5] /= texs;

                    meshVertices[edges[-faceEdges[j]].index1 * 6 + 4] =
                            meshVertices[edges[-faceEdges[j]].index1 * 6 + 0] * texture.uAxis.x +
                                    meshVertices[edges[-faceEdges[j]].index1 * 6 + 1] * texture.uAxis.y +
                                    meshVertices[edges[-faceEdges[j]].index1 * 6 + 2] * texture.uAxis.z +
                                    texture.uOffset;
                    meshVertices[edges[-faceEdges[j]].index1 * 6 + 4] /= texs;

                    meshVertices[edges[-faceEdges[j]].index1 * 6 + 5] =
                            meshVertices[edges[-faceEdges[j]].index1 * 6 + 0] * texture.vAxis.x +
                                    meshVertices[edges[-faceEdges[j]].index1 * 6 + 1] * texture.vAxis.y +
                                    meshVertices[edges[-faceEdges[j]].index1 * 6 + 2] * texture.vAxis.z +
                                    texture.vOffset;
                    meshVertices[edges[-faceEdges[j]].index1 * 6 + 5] /= texs;
                }

                faces[i].indicesCount += 2;
            }
        }

        mesh = new Mesh(true, meshVertices.length, meshIndices.length,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_color"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoords"));
        mesh.setVertices(meshVertices);
        mesh.setIndices(meshIndices);
    }

    public void render(ShaderProgram shader) {
        texture.bind();
        shader.setUniformi("u_texture", 0);

        // gross, have to do this because libgdx doesn't support glDrawArray in mesh render
        // nor primitive restart indices (yet?)
        for (int i = 0; i < faces.length; i++) {
            mesh.render(shader, GL20.GL_TRIANGLE_FAN, faces[i].indicesOffset, faces[i].indicesCount);
        }
    }

    private String readNextString(int length) throws IOException {
        stream.read(buffer, 0, length);

        return new String(buffer);
    }

    private int readNextInt() throws IOException {
        stream.read(buffer, 0, 4);

        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private float readNextFloat() throws IOException {
        stream.read(buffer, 0, 4);

        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    private short readNextShort() throws IOException {
        stream.read(buffer, 0, 2);

        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    private byte readNextByte() throws IOException {
        stream.read(buffer, 0, 1);

        return buffer[0];
    }

    class BspTextureInformation {
        BspPoint3f uAxis;
        float uOffset;
        BspPoint3f vAxis;
        float vOffset;
        int flags;
        int value;
        String textureName;
        int nextTextureInformation;
    }

    class BspPoint3f {
        float x;
        float y;
        float z;
    }

    class BspPlane {
        BspPoint3f normal;
        float distance;
        int type;
    }

    class BspEdge {
        short index1;
        short index2;
    }

    class BspFace {
        short plane;
        short planeSide;
        int firstEdge;
        short numEdges;
        short textureInfo;
        byte[] lightmapStyles = new byte[4];
        int lightmapOffset;

        // special properties for libgdx mesh rendering
        int indicesOffset;
        int indicesCount;
    }

    class BspVertex {
        float x;
        float y;
        float z;
    }

    class BspHeader {
        String magic;
        int version;
        BspLump[] lump = new BspLump[19];
    }

    class BspLump {
        int offset;
        int length;
    }
}
