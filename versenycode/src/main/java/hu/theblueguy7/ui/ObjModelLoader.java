package hu.theblueguy7.ui;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ObjModelLoader {

    public static MeshView load(InputStream input) throws IOException {
        List<float[]> vertices = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] parts = line.substring(2).trim().split("\\s+");
                    vertices.add(new float[]{
                            Float.parseFloat(parts[0]),
                            Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2])
                    });
                } else if (line.startsWith("vt ")) {
                    String[] parts = line.substring(3).trim().split("\\s+");
                    texCoords.add(new float[]{
                            Float.parseFloat(parts[0]),
                            1.0f - Float.parseFloat(parts[1])
                    });
                } else if (line.startsWith("f ")) {
                    String[] parts = line.substring(2).trim().split("\\s+");
                    // parse face indices
                    int[][] faceVerts = new int[parts.length][2];
                    for (int i = 0; i < parts.length; i++) {
                        String[] indices = parts[i].split("/");
                        faceVerts[i][0] = Integer.parseInt(indices[0]) - 1; // vertex index
                        faceVerts[i][1] = indices.length > 1 && !indices[1].isEmpty()
                                ? Integer.parseInt(indices[1]) - 1 : 0; // tex coord index
                    }
                    // triangulate
                    for (int i = 1; i < faceVerts.length - 1; i++) {
                        faces.add(new int[]{
                                faceVerts[0][0], faceVerts[0][1],
                                faceVerts[i][0], faceVerts[i][1],
                                faceVerts[i + 1][0], faceVerts[i + 1][1]
                        });
                    }
                }
            }
        }

        TriangleMesh mesh = new TriangleMesh();

        // add vertices
        float[] points = new float[vertices.size() * 3];
        for (int i = 0; i < vertices.size(); i++) {
            points[i * 3] = vertices.get(i)[0];
            points[i * 3 + 1] = vertices.get(i)[1];
            points[i * 3 + 2] = vertices.get(i)[2];
        }
        mesh.getPoints().addAll(points);

        // add tex coords
        if (texCoords.isEmpty()) {
            mesh.getTexCoords().addAll(0f, 0f);
        } else {
            float[] tc = new float[texCoords.size() * 2];
            for (int i = 0; i < texCoords.size(); i++) {
                tc[i * 2] = texCoords.get(i)[0];
                tc[i * 2 + 1] = texCoords.get(i)[1];
            }
            mesh.getTexCoords().addAll(tc);
        }

        // add faces
        int[] faceData = new int[faces.size() * 6];
        for (int i = 0; i < faces.size(); i++) {
            int[] f = faces.get(i);
            faceData[i * 6] = f[0];     // vertex 1
            faceData[i * 6 + 1] = f[1]; // tex 1
            faceData[i * 6 + 2] = f[2]; // vertex 2
            faceData[i * 6 + 3] = f[3]; // tex 2
            faceData[i * 6 + 4] = f[4]; // vertex 3
            faceData[i * 6 + 5] = f[5]; // tex 3
        }
        mesh.getFaces().addAll(faceData);

        MeshView meshView = new MeshView(mesh);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.web("#4a4b6a"));
        material.setSpecularColor(Color.web("#5c5d78"));
        material.setSpecularPower(60);
        meshView.setMaterial(material);

        return meshView;
    }
}
