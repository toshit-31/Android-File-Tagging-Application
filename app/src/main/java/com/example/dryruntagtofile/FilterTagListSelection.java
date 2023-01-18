package com.example.dryruntagtofile;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

public class FilterTagListSelection extends AppCompatActivity {

    CheckedTagsAdapter adapter;
    ArrayList<String> presentTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_tag_list_selection);

        Toolbar toolbar = findViewById(R.id.selection_search_toolbar);
        TextView searchBarText = findViewById(R.id.filter_search_bar_text);
        this.setSupportActionBar(toolbar);
        this.getSupportActionBar().setTitle("");


        ArrayList<String> tags = this.getIntent().getStringArrayListExtra("tags");
        ArrayList<String> availableTags = tags;
        Integer searchOperation = this.getIntent().getIntExtra("searchOperation", 0);

        presentTags = this.getIntent().getStringArrayListExtra("presentTags");
        ArrayList<String> exclusiveTags = this.getIntent().getStringArrayListExtra("exclusiveTags");
        if (exclusiveTags != null && exclusiveTags.size() > 0) {
            availableTags.removeAll(exclusiveTags);
        }

        adapter = new CheckedTagsAdapter(this, availableTags, presentTags);
        RecyclerView availableTagsView = findViewById(R.id.tag_selection_list);
        availableTagsView.setLayoutManager(new LinearLayoutManager(this));
        availableTagsView.setAdapter(adapter);

        findViewById(R.id.done_selection).setOnClickListener(view -> {
            ArrayList<String> selectedTags = new ArrayList<>();
//                selectedTags.addAll(filteredTags);
            selectedTags.addAll(adapter.getSelectedTags());
            Intent intent = new Intent();
            intent.putStringArrayListExtra("selected_tags", selectedTags);
            intent.putExtra("searchOperation", searchOperation);

            setResult(RESULT_OK, intent);
            finish();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.searchView);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                adapter.getFilter().filter(s);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}

class CheckedTagsAdapter extends RecyclerView.Adapter<CheckedTagsAdapter.ViewHolder> implements Filterable {

    Context ctx;
    List<String> tagsForFilter;
    List<String> availableTags;
    ArrayList<String> presentTags;
    HashSet<String> tagsFiltered = new HashSet<>();
    LayoutInflater inflater;

    public CheckedTagsAdapter(Context ctx, ArrayList<String> availableTags, ArrayList<String> presentTags){
        this.ctx = ctx;
        this.tagsForFilter = availableTags;
        this.presentTags = presentTags;
        this.availableTags = availableTags;
        this.inflater = LayoutInflater.from(ctx);
        tagsFiltered.addAll(presentTags);
    }

    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults filterResults = new FilterResults();
                try{
                    if (charSequence.length()==0 || charSequence == null){
                        filterResults.values = tagsForFilter;
                        filterResults.count = tagsForFilter.size();
                    }
                    else{
                        String searchChar = charSequence.toString().toLowerCase();
                        List<String> filteredResults = new ArrayList<>();
                        for(String tagName: tagsForFilter){
                            if(tagName.toLowerCase().contains(searchChar)){
                                filteredResults.add(tagName);
                            }
                        }
                        filterResults.values = filteredResults;
                        filterResults.count = filteredResults.size();
                    }
                }catch (Exception e) {
                    Log.d("Filter_result", e.toString());
                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                availableTags = (List<String>) filterResults.values;
                notifyDataSetChanged();
            }
        };
        return filter;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView tagName;
        CheckBox checkBox;
        androidx.cardview.widget.CardView tagHolder;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.tag_checkbox);
            tagName = itemView.findViewById(R.id.grid_tname);
            tagHolder = itemView.findViewById(R.id.tag_cont);
            itemView.findViewById(R.id.grid_image).setVisibility(View.GONE);
        }
    }


    @NonNull
    @Override
    public CheckedTagsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.row_tag, parent, false);
        return new CheckedTagsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckedTagsAdapter.ViewHolder holder, int position) {
        holder.tagName.setText(availableTags.get(position));
        holder.checkBox.setVisibility(View.VISIBLE);
        if(presentTags.size() > 0 && presentTags.contains(availableTags.get(position))){
            holder.checkBox.setChecked(true);
        }
        if(availableTags != null && availableTags.size() > 0) {
            /*holder.tagHolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (holder.checkBox.isChecked()) {
//                        filteredTags.add(tags.get(position));
                        tagsFiltered.add(tags.get(holder.getAdapterPosition()));
                    }
                    else {
//                        filteredTags.remove(tags.get(position));
                        tagsFiltered.remove(tags.get(holder.getAdapterPosition()));
                    }
                }
            });*/
            holder.checkBox.setOnClickListener(view -> {
                if (holder.checkBox.isChecked()) {
//                        filteredTags.add(tags.get(position));
                    tagsFiltered.add(availableTags.get(holder.getAdapterPosition()));
                }
                else {
//                        filteredTags.remove(tags.get(position));
                    tagsFiltered.remove(availableTags.get(holder.getAdapterPosition()));
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return availableTags.size();
    }

    public HashSet<String> getSelectedTags(){
        return tagsFiltered;
    }
}
