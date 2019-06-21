package ru.ystu.myystu;

import android.content.Context;

import androidx.room.Room;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;

import okhttp3.OkHttpClient;
import ru.ystu.myystu.Database.AppDatabase;

public class Application extends android.app.Application {

    private AppDatabase database;
    public static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        final Context context = Application.this;
        final OkHttpClient okHttpClient = new OkHttpClient();
        final ImagePipelineConfig config = OkHttpImagePipelineConfigFactory
                .newBuilder(context, okHttpClient)
                .build();

        Fresco.initialize(context, config);

        database = Room.databaseBuilder(this, AppDatabase.class, "myystudb").allowMainThreadQueries().build();
    }

    public static Application getInstance() {
        return instance;
    }
    public AppDatabase getDatabase() {
        return database;
    }
}
