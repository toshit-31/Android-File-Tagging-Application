package com.example.dryruntagtofile;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> implements Filterable {

    Context ctx;
    DiskDB diskDB;
    MemoryDB memoryDB;
    List<String> tags;
    List<String> tagsFiltered;
    Integer image;
    LayoutInflater inflater;
    TextView ediTagName;
    EditText editTag;
    ImageView btnEditClose;
    MaterialButton btnApplyChanges, btnDeleteTag;

    public GridAdapter(Context ctx, List<String> tags, Integer image){
        this.ctx = ctx;
        this.tags = tags;
        this.tagsFiltered = tags;
        this.image = image;
        this.inflater = LayoutInflater.from(ctx);
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults filterResults = new FilterResults();
                if (charSequence.length()==0 || charSequence == null){
                    filterResults.values = tagsFiltered;
                    filterResults.count = tagsFiltered.size();
                }
                else{
                    String searchChar = charSequence.toString().toLowerCase();
                    List<String> filteredResults = new ArrayList<>();
                    for(String tagName:tagsFiltered){
                        if(tagName.toLowerCase().contains(searchChar)){
                            filteredResults.add(tagName);
                        }
                    }
                    filterResults.values = filteredResults;
                    filterResults.count = filteredResults.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                tags = (List<String>) filterResults.values;
                notifyDataSetChanged();
            }
        };
        return filter;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView tagname;
        ImageView editIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tagname = itemView.findViewById(R.id.grid_tname);
            editIcon = itemView.findViewById(R.id.grid_image);

            editIcon.setOnClickListener(view -> {
                showEditDialog(tagname.getText().toString());
            });
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Toast.makeText(view.getContext(),"displaying contents of"+ tags.get(getAdapterPosition()),Toast.LENGTH_LONG).show();
                    diskDB = new DiskDB(ctx);
                    memoryDB = MemoryDB.getInstance(ctx);
                    Integer tagUID = memoryDB.getTagIdByName(tags.get(getAdapterPosition()));
                    String[] filesString = diskDB.getFilePathsFor(tagUID);
                    Intent intent = new Intent(ctx, FilterResult.class);
                    ArrayList<String> filesStringArrayList = new ArrayList<>();
                    filesStringArrayList.addAll(Arrays.asList(filesString));
                    intent.putStringArrayListExtra("filePaths", filesStringArrayList);
                    ctx.startActivity(intent);
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.row_tag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tagname.setText(tags.get(position));
        holder.editIcon.setImageResource(image);
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }


    private void showEditDialog(String tagName) {
        MemoryDB memoryDB = MemoryDB.getInstance(ctx);

        Dialog editDialog = new Dialog(ctx, R.style.dialog_theme);
        editDialog.setContentView(R.layout.edit_tag_layout);
        editDialog.setCancelable(true);
        editDialog.setCanceledOnTouchOutside(true);


        ediTagName = editDialog.findViewById(R.id.edit_tag_name);
        editTag = editDialog.findViewById(R.id.edit_name_field);
        btnEditClose = editDialog.findViewById(R.id.btn_edit_tag_close);
        btnApplyChanges = editDialog.findViewById(R.id.btn_apply_changes);
        btnDeleteTag = editDialog.findViewById(R.id.btn_delete_tag);

        ediTagName.setText(tagName);

        btnEditClose.setOnClickListener(view -> editDialog.dismiss());

        btnApplyChanges.setOnClickListener(view -> {
            String newTagName = editTag.getText().toString();
            if (newTagName.length()==0 || newTagName.equals("")){
                newTagName = tagName;
                editDialog.dismiss();
                Toast.makeText(view.getContext(),"Tag Unchanged",Toast.LENGTH_LONG).show();
            }
            if (tags.contains(newTagName)){
                newTagName = tagName;
                editDialog.dismiss();
                Toast.makeText(view.getContext(),"Tag Already Exists!",Toast.LENGTH_LONG).show();
            }
            memoryDB.updateTag( tagName,  newTagName);
            tags.remove(tagName);
            tags.add(newTagName);
            Toast.makeText(view.getContext(),"Tag Changed",Toast.LENGTH_LONG).show();
            editDialog.dismiss();
            notifyDataSetChanged();
        });

        btnDeleteTag.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setMessage("Are you sure you want to delete this tag?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                memoryDB.removeTag(tagName);
                                tags.remove(tagName);
                                Toast.makeText(view.getContext(),"Tag Deleted",Toast.LENGTH_LONG).show();
                                notifyDataSetChanged();
                            } catch (Exception e) {
                                Toast.makeText(view.getContext(),"Tag Could NOT be Deleted",Toast.LENGTH_LONG).show();
                                throw new RuntimeException(e);
                            }
                        }
                    }).setNegativeButton("Cancel",null);

            AlertDialog confirmDelete = builder.create();
            confirmDelete.show();

            editDialog.dismiss();
            notifyDataSetChanged();
        });
        editDialog.show();
    }

}