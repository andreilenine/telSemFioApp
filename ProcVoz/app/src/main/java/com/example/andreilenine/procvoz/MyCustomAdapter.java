package com.example.andreilenine.procvoz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by AndreiLenine on 27-4-17.
 */

public class MyCustomAdapter extends BaseAdapter implements ListAdapter{

    private ArrayList<String> list = new ArrayList<>();
    private Context context;
    private String source, turn;

    public MyCustomAdapter(Context context, ArrayList<String> list, String firstSource, String firstTurn) {
        this.context = context;
        this.list = list;
        source = firstSource;
        turn = firstTurn;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_view, null);
        }


        //Handle TextView and display string from your list
        TextView listItemText = (TextView) view.findViewById(R.id.player);
        listItemText.setText(list.get(position));

        if (list.get(position).equals(source)) {
            Button listenBtn = (Button) view.findViewById(R.id.listen);
            listenBtn.setVisibility(View.VISIBLE);
        }

        if (list.get(position).equals(turn)) {
            Button recordBtn = (Button) view.findViewById(R.id.record);
            recordBtn.setVisibility(View.VISIBLE);
        }

        //Handle buttons
        //ImageButton recordBtn = (ImageButton) view.findViewById(R.id.record);
        //ImageButton listenBtn = (ImageButton) view.findViewById(R.id.listen);

        return view;

    }

}
