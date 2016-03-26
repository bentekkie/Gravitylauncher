package com.mygdx.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Looper;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AndroidIcons implements MyGdxGame.appIcons {
    PackageManager mpm;
    Context mcontext;
    public AndroidIcons(PackageManager pm,Context context){
        mpm = pm;
        mcontext = context;
        msp = mcontext.getSharedPreferences("com.uselesslauncher.apps",Context.MODE_PRIVATE);
    }
    @Override
    public ArrayList<String> getIcons() {
        ArrayList<String> apps = new ArrayList<>();
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> availableActivities = mpm.queryIntentActivities(i, 0);
        for(ResolveInfo ri:availableActivities) {
            if(msp.getBoolean(ri.activityInfo.packageName,false)) {
                apps.add(ri.activityInfo.packageName);
            }
        }

        return apps;

    }
    public Texture bitmapTextureConverter(final Bitmap bitmap) {
        Texture tex = new Texture(bitmap.getWidth(), bitmap.getHeight(), Pixmap.Format.RGBA8888);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.getTextureObjectHandle());
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        bitmap.recycle();
        return tex;
    }

    @Override
    public void openApp(String packageName) {
        Intent intent = mpm.getLaunchIntentForPackage(packageName);
        if (intent == null) {
            // Bring user to the market or let them choose an app?
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mcontext.startActivity(intent);
    }

    @Override
    public Texture getIcon(String packageName) {
        try {
            return bitmapTextureConverter(convertToBitmap(mpm.getApplicationIcon(packageName), 200, 200));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    ArrayList<String> items;
    ArrayList<String> names;
    ArrayList<Boolean> selected;
    private boolean[] toPrimitiveArray(final List<Boolean> booleanList) {
        final boolean[] primitives = new boolean[booleanList.size()];
        int index = 0;
        for (Boolean object : booleanList) {
            primitives[index++] = object;
        }
        return primitives;
    }
    SharedPreferences msp;
    SharedPreferences.Editor meditor;
    @Override
    public ArrayList<String> selectApps() {
        items = new ArrayList<>();
        meditor = msp.edit();
        selected = new ArrayList<>();
        names = new ArrayList<>();
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> availableActivities = mpm.queryIntentActivities(i, 0);
        for(ResolveInfo ri:availableActivities) {
            items.add(ri.activityInfo.packageName);
            names.add(ri.activityInfo.loadLabel(mpm).toString());
            selected.add(msp.getBoolean(ri.activityInfo.packageName,false));
        }
        ((Activity) mcontext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(mcontext)
                        .setTitle("Add apps")
                        .setMultiChoiceItems(names.toArray(new String[names.size()]), toPrimitiveArray(selected), new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                selected.set(which, isChecked);
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for (int x = 0; x < selected.size(); x++) {
                                    meditor.putBoolean(items.get(x), selected.get(x));
                                    meditor.commit();


                                }
                                meditor.apply();
                            }
                        }).create().show();
            }
        });

        return null;
    }

    @Override
    public Texture getWallpaper() {
        WallpaperManager wm = WallpaperManager.getInstance(mcontext);
        return bitmapTextureConverter(convertToBitmap(wm.getDrawable(), Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
    }


    public Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heightPixels) {
        Bitmap mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mutableBitmap);
        drawable.setBounds(0, 0, widthPixels, heightPixels);
        drawable.draw(canvas);

        return mutableBitmap;
    }
}

