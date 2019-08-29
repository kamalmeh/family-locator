package com.smiansh.famtrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PlacesAdapter extends BaseAdapter {
    private Context context;
    private ArrayList mData;
    private Map<String, Object> tempMap = new HashMap<>();

    PlacesAdapter(Context ctx, Map<String, Object> map) {
        super();
        context = ctx;
        mData = new ArrayList();
        for (Map.Entry entry : map.entrySet()) {
            final DocumentReference documentReference = FirebaseFirestore.getInstance().document(entry.getValue().toString());

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Task<DocumentSnapshot> task = documentReference.get();
                    try {
                        DocumentSnapshot documentSnapshot = Tasks.await(task);
//                        GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                        String address = documentSnapshot.getString("address");
                        tempMap.put(documentSnapshot.getId(), address);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (tempMap != null) {
            //noinspection unchecked
            mData.addAll(tempMap.entrySet());
        }
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Map.Entry<String, String> getItem(int i) {
        //noinspection unchecked
        return (Map.Entry) mData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final View result;
        if (view == null) {
            result = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.places_list, viewGroup, false);
        } else {
            result = view;
        }

        Map.Entry<String, String> item = getItem(i);
        ((TextView) result.findViewById(R.id.placename)).setText(item.getKey());
        ((TextView) result.findViewById(R.id.placeaddress)).setText(item.getValue());

        return result;
    }
}
