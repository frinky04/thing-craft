/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resource;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Environment(value=EnvType.CLIENT)
public class ResourceDownloader
extends Thread {
    public File resourcesDir;
    private Minecraft minecraft;
    private boolean stopped = false;

    public ResourceDownloader(File gameDir, Minecraft minecraft) {
        this.minecraft = minecraft;
        this.setName("Resource download thread");
        this.setDaemon(true);
        this.resourcesDir = new File(gameDir, "resources/");
        if (!this.resourcesDir.exists() && !this.resourcesDir.mkdirs()) {
            throw new RuntimeException("The working directory could not be created: " + this.resourcesDir);
        }
    }

    public void run() {
        try {
            URL uRL = new URL("http://s3.amazonaws.com/MinecraftResources/");
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(uRL.openStream());
            NodeList nodeList = document.getElementsByTagName("Contents");
            for (int i = 0; i < 2; ++i) {
                for (int j = 0; j < nodeList.getLength(); ++j) {
                    Node node = nodeList.item(j);
                    if (node.getNodeType() != 1) continue;
                    Element element = (Element)node;
                    String string = ((Element)element.getElementsByTagName("Key").item(0)).getChildNodes().item(0).getNodeValue();
                    long l = Long.parseLong(((Element)element.getElementsByTagName("Size").item(0)).getChildNodes().item(0).getNodeValue());
                    if (l <= 0L) continue;
                    this.download(uRL, string, l, i);
                    if (!this.stopped) continue;
                    return;
                }
            }
        }
        catch (Exception exception) {
            this.loadResources(this.resourcesDir, "");
            exception.printStackTrace();
        }
    }

    public void forceReload() {
        this.loadResources(this.resourcesDir, "");
    }

    private void loadResources(File dir, String prefix) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; ++i) {
            if (files[i].isDirectory()) {
                this.loadResources(files[i], prefix + files[i].getName() + "/");
                continue;
            }
            try {
                this.minecraft.loadResource(prefix + files[i].getName(), files[i]);
                continue;
            }
            catch (Exception exception) {
                System.out.println("Failed to add " + prefix + files[i].getName());
            }
        }
    }

    private void download(URL url, String name, long size, int pass) {
        try {
            int i = name.indexOf("/");
            String string = name.substring(0, i);
            if (string.equals("sound") || string.equals("newsound") ? pass != 0 : pass != 1) {
                return;
            }
            File file = new File(this.resourcesDir, name);
            if (!file.exists() || file.length() != size) {
                file.getParentFile().mkdirs();
                String string2 = name.replaceAll(" ", "%20");
                this.download(new URL(url, string2), file, size);
                if (this.stopped) {
                    return;
                }
            }
            this.minecraft.loadResource(name, file);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void download(URL url, File dst, long size) {
        byte[] bs = new byte[4096];
        DataInputStream dataInputStream = new DataInputStream(url.openStream());
        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(dst));
        int i = 0;
        while ((i = dataInputStream.read(bs)) >= 0) {
            dataOutputStream.write(bs, 0, i);
            if (!this.stopped) continue;
            return;
        }
        dataInputStream.close();
        dataOutputStream.close();
    }

    public void shutdown() {
        this.stopped = true;
    }
}

