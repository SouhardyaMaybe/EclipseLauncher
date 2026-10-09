package me.shadow.eclipselauncher.pojav.value;

import androidx.annotation.Keep;

import me.shadow.eclipselauncher.pojav.JMinecraftVersionList.Arguments.ArgValue.ArgRules;

@Keep
public class DependentLibrary {
    public ArgRules[] rules;
    public String name;
    public LibraryDownloads downloads;
    public String url;

    @Keep
	public static class LibraryDownloads {
		public final MinecraftLibraryArtifact artifact;
		public LibraryDownloads(MinecraftLibraryArtifact artifact) {
			this.artifact = artifact;
		}

		/** No-arg constructor for JSON deserialization; the artifact is filled in reflectively when present. */
		public LibraryDownloads() {
			this(null);
		}
	}
}

