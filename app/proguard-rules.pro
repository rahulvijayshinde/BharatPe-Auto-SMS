# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep our data classes
-keep class com.autosms.bharatpe.data.db.TransactionEntity { *; }

# Compose
-dontwarn androidx.compose.**

# Coroutines
-dontwarn kotlinx.coroutines.**
