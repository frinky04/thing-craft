/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.texture;

import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.texture.HttpImageProcessor;

@Environment(value=EnvType.CLIENT)
public class HttpTexture {
    public BufferedImage image;
    public int count = 1;
    public int id = -1;
    public boolean uploaded = false;

    public HttpTexture(final String url, final HttpImageProcessor processor) {
        new Thread(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            public void run() {
                HttpURLConnection httpURLConnection;
                Object object = null;
                try {
                    URL uRL = new URL(url);
                    httpURLConnection = (HttpURLConnection)uRL.openConnection();
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(false);
                    httpURLConnection.connect();
                    if (httpURLConnection.getResponseCode() == 404) {
                        return;
                    }
                    HttpTexture.this.image = processor == null ? ImageIO.read(httpURLConnection.getInputStream()) : processor.process(ImageIO.read(httpURLConnection.getInputStream()));
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
                finally {
                    httpURLConnection.disconnect();
                }
            }
        }.start();
    }
}

