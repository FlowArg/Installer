/*
 * Installer
 * Copyright (c) 2016-2018.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.minecraftforge.installer.actions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;

import net.minecraftforge.installer.DownloadUtils;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;
import net.minecraftforge.installer.json.Version;
import net.minecraftforge.installer.json.Version.Download;

public class ClientInstall extends Action {

    public ClientInstall(Install profile, ProgressCallback monitor) {
        super(profile, monitor, true);
    }

    @Override
    public boolean run(File target, Predicate<String> optionals) throws ActionCanceledException {
        if (!target.exists()) {
            error("There is no minecraft installation at: " + target);
            return false;
        }

        File librariesDir = new File(target, "libraries");
        librariesDir.mkdir();

        this.checkCancel();

        // Extract version json
        monitor.stage("Extracting json");
        try (InputStream stream = Util.class.getResourceAsStream(profile.getJson())) {
            File json = new File(target, profile.getVersion() + ".json");
            json.getParentFile().mkdirs();
            Files.copy(stream, json.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            error("  Failed to extract");
            e.printStackTrace();
            return false;
        }
        this.checkCancel();

        File clientTarget = new File(target, "client.jar");
        if (!clientTarget.exists()) {
            File versionJson = new File(target, "client.json");
            Version vanilla = Util.getVanillaVersion(profile.getMinecraft(), versionJson);
            if (vanilla == null) {
                error("Failed to download version manifest, can not find client jar URL.");
                return false;
            }

            Download client = vanilla.getDownload("client");
            if (client == null) {
                error("Failed to download minecraft client, info missing from manifest: " + versionJson);
                return false;
            }

            if (!DownloadUtils.download(monitor, profile.getMirror(), client, clientTarget)) {
                clientTarget.delete();
                error("Downloading minecraft client failed, invalid checksum.\n" +
                      "Try again, or use the vanilla launcher to install the vanilla version.");
                return false;
            }
        }

        // Download Libraries
        if (!downloadLibraries(librariesDir, optionals))
            return false;
        this.checkCancel();

        if (!processors.process(librariesDir, clientTarget))
            return false;

        this.checkCancel();

        return true;
    }

    @Override
    public boolean isPathValid(File targetDir) {
        return targetDir.exists();
    }

    @Override
    public String getFileError(File targetDir) {
        if (targetDir.exists())
            return "The directory is missing a launcher profile. Please run the minecraft launcher first";
        else
            return "There is no minecraft directory set up. Either choose an alternative, or run the minecraft launcher to create one";
    }

    @Override
    public String getSuccessMessage() {
        if (downlaodedCount() > 0)
            return String.format("Successfully installed client profile %s for version %s into launcher, and downloaded %d libraries", profile.getProfile(), profile.getVersion(), downlaodedCount());
        return String.format("Successfully installed client profile %s for version %s into launcher", profile.getProfile(), profile.getVersion());
    }
}
