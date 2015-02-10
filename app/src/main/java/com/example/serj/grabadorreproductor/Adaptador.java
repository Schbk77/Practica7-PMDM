package com.example.serj.grabadorreproductor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class Adaptador extends ArrayAdapter<String>{
    private Context contexto;
    private ArrayList<String> canciones;
    private int recurso;
    private static LayoutInflater i;

    public static class ViewHolder {
        public TextView tv1;
    }

    public Adaptador(Context context, int resource, ArrayList<String> objects) {
        super(context, resource, objects);
        this.contexto = context;
        this.recurso = resource;
        this.canciones = objects;
        this.i = (LayoutInflater)contexto.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh =  null;
        if(convertView == null) {
            convertView = i.inflate(recurso, null);
            vh = new ViewHolder();
            vh.tv1 = (TextView)convertView.findViewById(R.id.tvCancion);
            convertView.setTag(vh);
        }else{
            vh = (ViewHolder)convertView.getTag();
        }
        vh.tv1.setText(canciones.get(position).toString());
        return convertView;
    }
}
