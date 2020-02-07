# Configure internal versions instead of system libraries
EXTRA_OECONF += " --disable-system-gio --disable-system-fontconfig"

# Remove build-time requirement for native builds
DEPENDS_remove = "glib-2.0-native fontconfig-native libxrender-native"
