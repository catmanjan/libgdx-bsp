package com.bsp;

import com.badlogic.gdx.files.FileHandle;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BspMap {

    public BspHeader header;
    public BspVertex[] vertices;
    public BspFace[] faces;
    public BspEdge[] edges;

    private int bufferSize = 1024;
    private BufferedInputStream stream;
    private byte[] buffer = new byte[bufferSize];

    public BspMap(FileHandle file) {
        try {
            load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load(FileHandle file) throws IOException {
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
            vertices[i].x = readNextFloat();
            vertices[i].y = readNextFloat();
            vertices[i].z = readNextFloat();
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

    class BspEdge {
        public short index1;
        public short index2;
    }

    class BspFace {
        public short plane;
        public short planeSide;
        public int firstEdge;
        public short numEdges;
        public short textureInfo;
        public byte[] lightmapStyles = new byte[4];
        public int lightmapOffset;
    }

    class BspVertex {
        public float x;
        public float y;
        public float z;
    }

    class BspHeader {
        public String magic;
        public int version;
        public BspLump[] lump = new BspLump[19];
    }

    class BspLump {
        public int offset;
        public int length;
    }

}
