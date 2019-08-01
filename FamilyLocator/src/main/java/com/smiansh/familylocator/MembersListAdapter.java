package com.smiansh.familylocator;

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

public class MembersListAdapter extends BaseAdapter {
    private final ArrayList mData;
    Map<String, Object> tempMap = new HashMap<>();

    MembersListAdapter(Map<String, String> map) {
        mData = new ArrayList();
        for (Map.Entry entry : map.entrySet()) {
            final DocumentReference documentReference = FirebaseFirestore.getInstance().document(entry.getValue().toString());

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Task<DocumentSnapshot> task = documentReference.get();
                    try {
                        DocumentSnapshot documentSnapshot = Tasks.await(task);
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        //noinspection unchecked
                        tempMap.put(firstName + " " + lastName, documentSnapshot.getId());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
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
            result = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.members_list, viewGroup, false);
        } else {
            result = view;
        }

        Map.Entry<String, String> item = getItem(i);
        ((TextView) result.findViewById(R.id.membername)).setText(item.getKey());

        return result;
    }
}
