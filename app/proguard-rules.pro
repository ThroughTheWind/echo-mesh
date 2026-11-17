# No plaintext logs on a phone that may be seized (brief, constraint 5): R8 removes every
# android.util.Log call from release builds, so a stray log line cannot leak a position or a key.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int println(...);
    public static boolean isLoggable(java.lang.String, int);
}
