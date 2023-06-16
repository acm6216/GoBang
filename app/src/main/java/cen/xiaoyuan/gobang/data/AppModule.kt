package cen.xiaoyuan.gobang.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cen.xiaoyuan.gobang.adapters.GoBangGameTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private lateinit var appDatabase: AppDatabase
    private lateinit var gson:Gson

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        appDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "game_provide.db"
        ).fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {}).build()
        return appDatabase
    }

    @Provides
    fun provideGameDao(appDatabase: AppDatabase): GameDao {
        return appDatabase.gameDao()
    }

    @Provides
    @Singleton
    fun provideGson():Gson {
        gson = GsonBuilder().registerTypeAdapter(Chess::class.java, GoBangGameTypeAdapter()).create()
        return gson
    }

}