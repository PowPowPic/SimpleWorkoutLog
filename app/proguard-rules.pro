# SWL app-specific R8 / ProGuard rules
#
# Keep this file intentionally minimal.
# Room, Google Play Billing, and In-App Review provide their own consumer rules.
# The app does not use reflection-based model serialization.
#
# Do NOT add broad rules such as:
#   -keep class kotlin.** { *; }
# because they prevent R8 from optimizing and obfuscating a large part of the app.
