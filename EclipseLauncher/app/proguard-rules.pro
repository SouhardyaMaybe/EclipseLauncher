# Keep crash reports useful without shipping source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# The game and installer activities are named in AndroidManifest.xml. AGP
# keeps manifest components, but state it here so a future shrinker change does
# not turn a class rename into an ActivityNotFoundException.
-keep class dev.shadow.eclipselauncher.host.GameActivity { <init>(...); }
-keep class dev.shadow.eclipselauncher.host.InstallerActivity { <init>(...); }
