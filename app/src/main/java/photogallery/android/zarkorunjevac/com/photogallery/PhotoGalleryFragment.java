package photogallery.android.zarkorunjevac.com.photogallery;


import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.support.v4.app.Fragment;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by zarkorunjevac on 13/12/14.
 */
public class PhotoGalleryFragment extends Fragment {

    private static String TAG;
    ArrayList<GalleryItem> mItems;

    static {
        TAG = PhotoGalleryFragment.class.getSimpleName();
    }

    GridView mGridView;
    SwipeRefreshLayout mSwipeRefreshLayout;
    ThumbnailDownloader<ImageView> mThumbnailThread;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        new FetchItemsTask().execute();

        //mThumbnailThread=new ThumbnailDownloader<ImageView>();
        mThumbnailThread=new ThumbnailDownloader<ImageView>(new Handler());
        mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            @Override
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
                if(isVisible()){
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG,"Background thread started");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_photo_gallery,container,false);

        mGridView=(GridView)v.findViewById(R.id.gridView);
        mSwipeRefreshLayout=(SwipeRefreshLayout) v.findViewById(R.id.activity_main_swipe_refresh_layout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new FetchItemsTask().execute();
            }
        });

        setupAdapter();
        return v;
    }

    void setupAdapter(){
        if(null==getActivity() || mGridView==null) return;

        if(null!=mItems){
            //mGridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(),
            //        android.R.layout.simple_gallery_item,mItems));

            mGridView.setAdapter(new GalleryItemAdapter(mItems));
            mSwipeRefreshLayout.setRefreshing(false);
        }
        else{
            mGridView.setAdapter(null);
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i(TAG,"Background thread destroyed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_photo_gallery,menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_settings:
                // do s.th.
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }


    }

    private class FetchItemsTask extends AsyncTask<Void,Void,ArrayList<GalleryItem>>{
        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {


            return new FlickrFetchr().fetchItems();
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            mItems=items;
            setupAdapter();
        }
    }

   private class GalleryItemAdapter extends ArrayAdapter<GalleryItem>{

       public GalleryItemAdapter(ArrayList<GalleryItem> items){
           super(getActivity(),0,items);
       }

       @Override
       public View getView(int position, View convertView, ViewGroup parent) {

           if(null==convertView){
               convertView=getActivity().getLayoutInflater().inflate(R.layout.gallery_item,parent,false);
           }

           ImageView imageView=(ImageView)convertView.findViewById(R.id.gallery_item_imageView);
           imageView.setImageResource(R.drawable.brian_up_close);

           GalleryItem item=getItem(position);
           mThumbnailThread.queueThumbnail(imageView,item.getUrl());

           return  convertView;

       }
   }

}
