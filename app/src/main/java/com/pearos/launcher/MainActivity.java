package com.pearos.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int REQ_WALL = 7;
    float d;
    int sw, sh;
    boolean resumed = false;
    FrameLayout root;
    LinearLayout tray;
    HorizontalScrollView trayScroll;
    List<ResolveInfo> apps = new ArrayList<ResolveInfo>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        d = getResources().getDisplayMetrics().density;
        sw = getResources().getDisplayMetrics().widthPixels;
        sh = getResources().getDisplayMetrics().heightPixels;
        PackageManager pm = getPackageManager();

        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
            if (!ri.activityInfo.packageName.equals(getPackageName())) apps.add(ri);
        }
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));

        root = new FrameLayout(this);
        applyWallpaper();

        View.OnLongClickListener wl = new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                chooseWallpaper();
                return true;
            }
        };

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(0, (int) (44 * d), 0, (int) (120 * d));
        for (ResolveInfo ri : apps) grid.addView(tile(ri, sw / 4, false));
        grid.setOnLongClickListener(wl);
        ScrollView sv = new ScrollView(this);
        sv.addView(grid);
        sv.setOnLongClickListener(wl);
        root.addView(sv);
        root.setOnLongClickListener(wl);

        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(0, (int) (8 * d), 0, (int) (8 * d));
        dock.setBackgroundDrawable(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xCC5A5F6B, 0xEE15171C}));
        for (int i = 0; i < Math.min(4, apps.size()); i++) dock.addView(tile(apps.get(i), sw / 4, true));
        root.addView(dock, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        tray = new LinearLayout(this);
        tray.setGravity(Gravity.CENTER_VERTICAL);
        tray.setPadding((int) (8 * d), (int) (6 * d), (int) (8 * d), (int) (6 * d));
        trayScroll = new HorizontalScrollView(this);
        trayScroll.setHorizontalScrollBarEnabled(false);
        trayScroll.setBackgroundDrawable(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xEE30343C, 0xF0101216}));
        trayScroll.addView(tray);
        trayScroll.setVisibility(View.GONE);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        tp.bottomMargin = (int) (90 * d);
        root.addView(trayScroll, tp);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (resumed) {
            if (trayScroll.getVisibility() == View.VISIBLE) hideTray(); else showTray();
        } else {
            hideTray();
        }
    }

    @Override
    public void onBackPressed() {
        hideTray();
    }

    void hideTray() {
        if (trayScroll != null) trayScroll.setVisibility(View.GONE);
    }

    void showTray() {
        tray.removeAllViews();
        boolean any = false;
        for (String pkg : getRecents()) {
            ResolveInfo ri = find(pkg);
            if (ri == null) continue;
            tray.addView(tile(ri, (int) (80 * d), false));
            any = true;
        }
        if (!any) {
            TextView t = new TextView(this);
            t.setText("No recent apps yet");
            t.setTextColor(Color.WHITE);
            t.setPadding((int) (12 * d), (int) (24 * d), (int) (12 * d), (int) (24 * d));
            tray.addView(t);
        }
        trayScroll.setVisibility(View.VISIBLE);
    }

    ResolveInfo find(String pkg) {
        for (ResolveInfo ri : apps) {
            if (ri.activityInfo.packageName.equals(pkg)) return ri;
        }
        return null;
    }

    List<String> getRecents() {
        String s = getSharedPreferences("pearos", 0).getString("recents", "");
        List<String> r = new ArrayList<String>();
        if (s.length() > 0) r.addAll(Arrays.asList(s.split(",")));
        return r;
    }

    void launch(String pkg) {
        List<String> r = getRecents();
        r.remove(pkg);
        r.add(0, pkg);
        while (r.size() > 8) r.remove(r.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (String s : r) {
            if (sb.length() > 0) sb.append(',');
            sb.append(s);
        }
        getSharedPreferences("pearos", 0).edit().putString("recents", sb.toString()).apply();
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) startActivity(i);
    }

    void showInfo(final ResolveInfo ri) {
        final String pkg = ri.activityInfo.packageName;
        String msg = "Package: " + pkg;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(pkg, 0);
            msg = "Package: " + pkg
                + "\nVersion: " + pi.versionName
                + "\nInstalled: " + DateFormat.getDateInstance().format(new Date(pi.firstInstallTime));
        } catch (Exception e) {
        }
        new AlertDialog.Builder(this)
            .setTitle(ri.loadLabel(getPackageManager()))
            .setMessage(msg)
            .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int w) {
                    launch(pkg);
                }
            })
            .setNeutralButton("App settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int w) {
                    try {
                        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        i.setData(Uri.parse("package:" + pkg));
                        startActivity(i);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Can't open settings", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    void chooseWallpaper() {
        new AlertDialog.Builder(this)
            .setTitle("Wallpaper")
            .setItems(new String[]{"Choose a photo", "Reset to linen"}, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int which) {
                    if (which == 0) {
                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.setType("image/*");
                        try {
                            startActivityForResult(i, REQ_WALL);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(MainActivity.this, "No photo picker found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        new File(getFilesDir(), "wallpaper.jpg").delete();
                        applyWallpaper();
                    }
                }
            })
            .show();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_WALL && res == RESULT_OK && data != null && data.getData() != null) {
            try {
                Bitmap bm = loadScaled(data.getData(), sw, sh);
                FileOutputStream fo = new FileOutputStream(new File(getFilesDir(), "wallpaper.jpg"));
                bm.compress(Bitmap.CompressFormat.JPEG, 90, fo);
                fo.close();
                applyWallpaper();
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't set wallpaper", Toast.LENGTH_SHORT).show();
            }
        }
    }

    Bitmap loadScaled(Uri u, int tw, int th) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        InputStream in = getContentResolver().openInputStream(u);
        BitmapFactory.decodeStream(in, null, o);
        in.close();
        int s = 1;
        while (o.outWidth / (s * 2) >= tw && o.outHeight / (s * 2) >= th) s *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = s;
        in = getContentResolver().openInputStream(u);
        Bitmap raw = BitmapFactory.decodeStream(in, null, o);
        in.close();
        float sc = Math.max((float) tw / raw.getWidth(), (float) th / raw.getHeight());
        Bitmap out = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Matrix m = new Matrix();
        m.postScale(sc, sc);
        m.postTranslate((tw - raw.getWidth() * sc) / 2f, (th - raw.getHeight() * sc) / 2f);
        c.drawBitmap(raw, m, new Paint(Paint.FILTER_BITMAP_FLAG));
        return out;
    }

    void applyWallpaper() {
        File f = new File(getFilesDir(), "wallpaper.jpg");
        Bitmap bm = f.exists() ? BitmapFactory.decodeFile(f.getAbsolutePath()) : null;
        if (bm != null) root.setBackgroundDrawable(new BitmapDrawable(getResources(), bm));
        else root.setBackgroundDrawable(linen());
    }

    View tile(final ResolveInfo ri, int w, boolean dock) {
        PackageManager pm = getPackageManager();
        int size = (int) (58 * d);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(0, (int) (10 * d), 0, (int) (6 * d));
        col.setLayoutParams(new ViewGroup.LayoutParams(w, -2));
        col.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideTray();
                launch(ri.activityInfo.packageName);
            }
        });
        col.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                showInfo(ri);
                return true;
            }
        });
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(new Gloss(ri.loadIcon(pm)));
        col.addView(iv, new LinearLayout.LayoutParams(size, size));
        if (!dock) {
            TextView t = new TextView(this);
            t.setText(ri.loadLabel(pm));
            t.setTextSize(11f);
            t.setMaxLines(1);
            t.setGravity(Gravity.CENTER);
            t.setTextColor(Color.WHITE);
            t.setShadowLayer(2f * d, 0f, d, Color.BLACK);
            col.addView(t);
        }
        return col;
    }

    Drawable linen() {
        int s = 96;
        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(0xFF4C515B);
        Random r = new Random(6);
        Paint p = new Paint();
        for (int i = 0; i < s; i++) {
            p.setColor(r.nextBoolean() ? Color.argb(18 + r.nextInt(20), 255, 255, 255)
                                       : Color.argb(18 + r.nextInt(20), 0, 0, 0));
            c.drawLine(0, i, s, i, p);
            p.setColor(r.nextBoolean() ? Color.argb(18 + r.nextInt(20), 255, 255, 255)
                                       : Color.argb(18 + r.nextInt(20), 0, 0, 0));
            c.drawLine(i, 0, i, s, p);
        }
        BitmapDrawable bd = new BitmapDrawable(getResources(), bmp);
        bd.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        return bd;
    }

    static class Gloss extends Drawable {
        Drawable icon;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Gloss(Drawable icon) { this.icon = icon; }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            float w = b.width();
            RectF rect = new RectF(b);
            float r = w * 0.22f;
            Path clip = new Path();
            clip.addRoundRect(rect, r, r, Path.Direction.CW);
            c.save();
            c.clipPath(clip);
            p.setColor(0xFF2A2A2A);
            c.drawRect(rect, p);
            icon.setBounds(b.left, b.top, b.right, b.bottom);
            icon.draw(c);
            p.setShader(new LinearGradient(0, rect.top, 0, rect.centerY(),
                0x99FFFFFF, 0x14FFFFFF, Shader.TileMode.CLAMP));
            c.drawOval(new RectF(rect.left - w * .25f, rect.top - w * .6f,
                rect.right + w * .25f, rect.top + w * .5f), p);
            p.setShader(null);
            c.restore();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f);
            p.setColor(0x66000000);
            c.drawRoundRect(rect, r, r, p);
            p.setStyle(Paint.Style.FILL);
        }

        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
                                                                            }
