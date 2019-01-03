package com.project.stephencao.imagemagnifier.activity;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.project.stephencao.imagemagnifier.R;
import com.project.stephencao.imagemagnifier.view.ZoomImageView;

public class MainActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private int[] mImages = new int[]{R.mipmap.trump,R.mipmap.trump2,R.mipmap.trump3};
    private ImageView[] mImageViews = new ImageView[mImages.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager);
        mViewPager = findViewById(R.id.id_viewpager);
        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return mImageViews.length;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
               container.removeView(mImageViews[position]);
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                ZoomImageView zoomImageView = new ZoomImageView(MainActivity.this);
                zoomImageView.setImageResource(mImages[position]);
                container.addView(zoomImageView);
                mImageViews[position] = zoomImageView;
                return zoomImageView;
            }
        });
    }
}
