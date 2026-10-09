package me.shadow.eclipselauncher.core.tasks;

import static me.shadow.eclipselauncher.core.utils.DownloadUtils.downloadString;

import androidx.annotation.Nullable;

import me.shadow.eclipselauncher.json.JsonIOException;
import me.shadow.eclipselauncher.json.JsonSyntaxException;
import me.shadow.eclipselauncher.feature.log.Logging;
import me.shadow.eclipselauncher.task.Task;
import me.shadow.eclipselauncher.utils.path.PathManager;
import me.shadow.eclipselauncher.utils.EclipseTools;
import me.shadow.eclipselauncher.utils.path.UrlManager;

import me.shadow.eclipselauncher.core.JMinecraftVersionList;
import me.shadow.eclipselauncher.core.Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

/** Class getting the version list, and that's all really */
public class AsyncVersionList {

    public void getVersionList(@Nullable VersionDoneListener listener, boolean secondPass){
        Task.runTask(() -> {
            File versionFile = new File(PathManager.FILE_VERSION_LIST);
            JMinecraftVersionList versionList = null;
            try {
                if (!versionFile.exists() || (EclipseTools.getCurrentTimeMillis() > versionFile.lastModified() + 86400000)) {
                    versionList = downloadVersionList(UrlManager.URL_MINECRAFT_VERSION_REPOS);
                }
            } catch (Exception e) {
                Logging.e("AsyncVersionList", "Refreshing version list failed :" + e);
                Logging.e("GetVersionList", Tools.printToString(e));
            }

            // Fallback when no network or not needed
            if (versionList == null) {
                try {
                    versionList = Tools.JSON_CODEC.fromJson(new FileReader(versionFile), JMinecraftVersionList.class);
                } catch (FileNotFoundException e) {
                    Logging.e("File Not Found", Tools.printToString(e));
                } catch (JsonIOException | JsonSyntaxException e) {
                    Logging.e("AsyncVersionList", Tools.printToString(e));
                    versionFile.delete();
                    if (!secondPass) getVersionList(listener, true);
                }
            }

            if (listener != null) listener.onVersionDone(versionList);
            return null;
        }).execute();
    }


    @SuppressWarnings("SameParameterValue")
    private JMinecraftVersionList downloadVersionList(String mirror) {
        JMinecraftVersionList list = null;
        try {
            Logging.i("ExtVL", "Syncing to external: " + mirror);
            String jsonString = downloadString(mirror);
            list = Tools.JSON_CODEC.fromJson(jsonString, JMinecraftVersionList.class);
            Logging.i("ExtVL", "Downloaded the version list, len=" + list.versions.length);

            // Then save the version list
            //TODO make it not save at times ?
            FileOutputStream fos = new FileOutputStream(PathManager.FILE_VERSION_LIST);
            fos.write(jsonString.getBytes());
            fos.close();
        } catch (IOException e) {
            Logging.e("AsyncVersionList", Tools.printToString(e));
        }
        return list;
    }

    /** Basic listener, acting as a callback */
    public interface VersionDoneListener{
        void onVersionDone(JMinecraftVersionList versions);
    }

}
