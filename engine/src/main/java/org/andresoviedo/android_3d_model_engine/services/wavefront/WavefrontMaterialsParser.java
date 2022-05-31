package org.andresoviedo.android_3d_model_engine.services.wavefront;

import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.Material;
import org.andresoviedo.android_3d_model_engine.model.Materials;
import org.andresoviedo.util.math.Math3DUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
/**************************************************************************************************/
final class WavefrontMaterialsParser {
    /**********************************************************************************************/
    Materials parse(String id, InputStream inputStream) {
        Log.i("WavefrontMaterialsParse", "Parsing materials... ");

        final Materials materials = new Materials(id);

        try {
            final BufferedReader isReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;

            Material currMaterial = new Material();
            boolean createNewMaterial = false;

            while (((line = isReader.readLine()) != null)) {
                line = line.trim();

                if (line.length() == 0) {
                    continue;
                }

                if (line.startsWith("newmtl ")) {
                    if (createNewMaterial) {
                        materials.add(currMaterial.getName(), currMaterial);
                        currMaterial = new Material();
                    }

                    createNewMaterial = true;

                    currMaterial.setName(line.substring(6).trim());

                    Log.d("WavefrontMaterialsParse", "New material found: " + currMaterial.getName());

                } else if (line.startsWith("map_Kd ")) {
                    currMaterial.setTextureFile(line.substring(6).trim());

                    Log.v("WavefrontMaterialsParse", "Texture found: " + currMaterial.getTextureFile());

                } else if (line.startsWith("Ka ")) {
                    currMaterial.setAmbient(Math3DUtils.parseFloat(line.substring(2).trim().split(" ")));

                    Log.v("WavefrontMaterialsParse", "Ambient color: " + Arrays.toString(currMaterial.getAmbient()));
                } else if (line.startsWith("Kd ")) {
                    currMaterial.setDiffuse(Math3DUtils.parseFloat(line.substring(2).trim().split(" ")));

                    Log.v("WavefrontMaterialsParse", "Diffuse color: " + Arrays.toString(currMaterial.getDiffuse()));
                } else if (line.startsWith("Ks ")) {
                    currMaterial.setSpecular(Math3DUtils.parseFloat(line.substring(2).trim().split(" ")));

                    Log.v("WavefrontMaterialsParse", "Specular color: " + Arrays.toString(currMaterial.getSpecular()));
                } else if (line.startsWith("Ns ")) {
                    float val = Float.parseFloat(line.substring(3));

                    currMaterial.setShininess(val);

                    Log.v("WavefrontMaterialsParse", "Shininess: " + currMaterial.getShininess());
                } else if (line.charAt(0) == 'd') {
                    float val = Float.parseFloat(line.substring(2));

                    currMaterial.setAlpha(val);

                    Log.v("WavefrontMaterialsParse", "Alpha: " + currMaterial.getAlpha());
                } else if (line.startsWith("Tr ")) {
                    currMaterial.setAlpha(1 - Float.parseFloat(line.substring(3)));

                    Log.v("WavefrontMaterialsParse", "Transparency (1-Alpha): " + currMaterial.getAlpha());
                } else if (line.startsWith("illum ")) {
                    Log.v("WavefrontMaterialsParse", "Ignored line: " + line);
                } else if (line.charAt(0) == '#') {
                    Log.v("WavefrontMaterialsParse", line);
                } else {
                    Log.v("WavefrontMaterialsParse", "Ignoring line: " + line);
                }
            }

            materials.add(currMaterial.getName(), currMaterial);
        } catch (Exception e) {
            Log.e("WavefrontMaterialsParse", e.getMessage(), e);
        }

        Log.i("WavefrontMaterialsParse", "Parsed materials: " + materials);

        return materials;
    }
}
