package com.pearos.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    float d;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        d = getResources().getDisplayMetrics().density;
        int sw = getResources().getDisplayMetrics().widthPixels;
        PackageManager pm = getPackageManager();

        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = new ArrayList<ResolveInfo>();
        for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
            if (!ri.activityInfo.packageName.equals(getPackageName())) apps.add(ri);
        }
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundDrawable(linen());

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(0, (int) (44 * d), 0, (int) (120 * d));
        for (ResolveInfo ri : apps) grid.addView(tile(ri, sw / 4, false));
        ScrollView sv = new ScrollView(this);
        sv.addView(grid);
        root.addView(sv);

        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(0, (int) (8 * d), 0, (int) (8 * d));
        dock.setBackgroundDrawable(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xCC5A5F6B, 0xEE15171C}));
        for (int i = 0; i < Math.min(4, apps.size()); i++) dock.addView(tile(apps.get(i), sw / 4, true));
        root.addView(dock, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        setContentView(root);
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
                Intent i = getPackageManager().getLaunchIntentForPackage(ri.activityInfo.packageName);
                if (i != null) startActivity(i);
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
