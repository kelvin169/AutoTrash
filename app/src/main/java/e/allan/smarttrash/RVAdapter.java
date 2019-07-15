package e.allan.smarttrash;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.MyViewHolder> {

    List<Map<String, String>> dataList;
    Context context;

    public RVAdapter(Context context, List<Map<String, String>> dataList){
        this.dataList = dataList;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_item, viewGroup, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int i) {
        Map<String, String> currentData = dataList.get(i);
        String location = currentData.get("location");
        holder.tvLocation.setText(context.getResources().getString(R.string.location_link, location));
        holder.tvLocation.setText(Html.fromHtml("<a href=\""+ location + "\">" + "View Location" + "</a>"));
//        tv1.setClickable(true);
        holder.tvLocation.setMovementMethod (LinkMovementMethod.getInstance());
        holder.tvTime.setText(currentData.get("time"));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void addData(Map<String, String> data){
        dataList.add(data);
        notifyDataSetChanged();
    }

    class MyViewHolder extends RecyclerView.ViewHolder{
        TextView tvLocation, tvTime;
        ImageView imgCheck;
        MyViewHolder(View itemView){
            super(itemView);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvTime = itemView.findViewById(R.id.tv_time);
            imgCheck = itemView.findViewById(R.id.img_item_check);
            imgCheck.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    int adapterPosition = getAdapterPosition();
//                    if(adapterPosition != RecyclerView.NO_POSITION){
//                        dataList.remove(adapterPosition);
//                        notifyItemChanged(adapterPosition);
//                    }

                }
            });
        }
    }

}
