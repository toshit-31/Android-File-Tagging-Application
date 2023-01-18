package com.example.dryruntagtofile;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.WINDOW_SERVICE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class SettingPopup {

    Context ctx;
    PopupWindow popup;
    View popupRoot;
    View view;
    MyAdapter.ViewHolder itemView_MyAdapter;
    FilterResultAdapter.ViewHolder itemView_FilterResultAdapter;
    MemoryDB memdb;
    ArrayList<String> fileTags = new ArrayList<>();
    ArrayAdapter<String> availableTagsAdapter;
    private File file;
    boolean tagAddFlag = false; // f -> open text input , t -> save tag

    // to know if a setting popup is open
    static boolean open = false;

    public SettingPopup(Context context, final View view, MemoryDB memdb){
        this.ctx = context;
        this.view = view;
        this.memdb = memdb;
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(LAYOUT_INFLATER_SERVICE);
        popupRoot = inflater.inflate(R.layout.setting_popup, null);
        // popup = new PopupWindow(popupRoot, 800, 1500, true);
        popup = new PopupWindow(popupRoot, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);
        availableTagsAdapter = new ArrayAdapter<String>(context, R.layout.list_item, R.id.availTagTextView);
    }

    private TextView getTextView(int res){
        return (TextView) popupRoot.findViewById(res);
    }

    private void refreshFileTagList(){
        String filePath = file.getAbsolutePath();
        FlexboxLayout tagContainer = (FlexboxLayout) popupRoot.findViewById(R.id.tag_container);
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(LAYOUT_INFLATER_SERVICE);
        tagContainer.removeAllViews();
        fileTags.clear();
        try {
            fileTags.addAll(Arrays.asList(memdb.getTagsForFile(filePath)));
        } catch (Exception e){

        }
        for(String ftag : fileTags){
            addTagChip(tagContainer, ftag, filePath);
           /* LinearLayout tagChip = (LinearLayout) inflater.inflate(R.layout.tag_chip, null);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMarginStart(5);
            p.setMarginEnd(5);
            tagChip.setLayoutParams(p);
            TextView tagName = tagChip.findViewById(R.id.tag_name);
            ImageButton removeBtn = tagChip.findViewById(R.id.remove_tag);
            tagName.setText(ftag);
            removeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        memdb.removeTagFromFile(filePath, ftag);
                    } catch (Exception e) {

                    } finally {
                        fileTags.remove(ftag);
                        tagContainer.removeView(tagChip);
                        if(fileTags.size() == 0){
                            tagContainer.setVisibility(View.GONE);
                            popupRoot.findViewById(R.id.no_tag_text).setVisibility(View.VISIBLE);
                        }
                        updateTagLists();
                    }
                }
            });
            tagContainer.addView(tagChip);*/
        }
    }

    private void addTagChip(FlexboxLayout tagContainer, String ftag, String filePath){
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(LAYOUT_INFLATER_SERVICE);
        LinearLayout tagChip = (LinearLayout) inflater.inflate(R.layout.tag_chip, null);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMarginStart(5);
        p.setMarginEnd(5);
        tagChip.setLayoutParams(p);
        TextView tagName = tagChip.findViewById(R.id.tag_name);
        ImageButton removeBtn = tagChip.findViewById(R.id.remove_tag);
        tagName.setText(ftag);
        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    memdb.removeTagFromFile(filePath, ftag);
                } catch (Exception e) {

                } finally {
                    fileTags.remove(ftag);
                    tagContainer.removeView(tagChip);
                    if(fileTags.size() == 0){
                        tagContainer.setVisibility(View.GONE);
                        popupRoot.findViewById(R.id.no_tag_text).setVisibility(View.VISIBLE);
                    }
                    updateTagLists();
                }
            }
        });
        tagContainer.addView(tagChip);
    }

    private void refreshAvailableTagList(){
        ArrayList<String> availableTags = memdb.getTags();
        for(int i = 0; i < fileTags.size(); i++){
            String tagName = fileTags.get(i);
            if(fileTags.contains(tagName)) availableTags.remove(tagName);
        }
        availableTags.sort(new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.compareToIgnoreCase(t1);
            }
        });
        availableTagsAdapter.clear();
        availableTagsAdapter.addAll(availableTags);
    }

    private void updateTagLists() {
        refreshFileTagList();
        refreshAvailableTagList();
    }

    private void loadData(String filePath){
        file = new File(filePath);
        getTextView(R.id.file_name).setText(file.getName());
        String fileSize = new DecimalFormat("0.00").format(((float)file.length())/1048576);
        String date =  DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.UK).format(new Date(file.lastModified()));
        getTextView(R.id.meta_info).setText(fileSize+"mb | "+date);

        // getting tags attached with it

        FlexboxLayout tagContainer = (FlexboxLayout) popupRoot.findViewById(R.id.tag_container);
        updateTagLists();
        if(!fileTags.isEmpty()){
            popupRoot.findViewById(R.id.no_tag_text).setVisibility(View.GONE);
        } else {
            tagContainer.setVisibility(View.GONE);
            popupRoot.findViewById(R.id.no_tag_text).setVisibility(View.VISIBLE);
        }

        ListView availableTagsList = (ListView) popupRoot.findViewById(R.id.avail_tag_list);
        availableTagsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String tagName = (String)((TextView)view).getText();
                try {
                    memdb.addTagToFile(filePath, tagName);
                    fileTags.add(tagName);
                    tagContainer.setVisibility(View.VISIBLE);
                    popupRoot.findViewById(R.id.no_tag_text).setVisibility(View.GONE);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                } finally {
                    updateTagLists();
                }
            }
        });
        availableTagsList.setAdapter(availableTagsAdapter);

        // add tags functionality
        EditText tagNameInput = (EditText) popupRoot.findViewById(R.id.tag_name_input);
        popupRoot.findViewById(R.id.add_tag_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tagAddFlag){
                    popupRoot.findViewById(R.id.popup_base).setVisibility(View.GONE);
                    ((Button) view).setText("add tag");
                    String tagName = tagNameInput.getText().toString();
                    try{
                        memdb.addTag(tagName);
                        memdb.addTagToFile(file.getAbsolutePath(), tagName);
                        updateTagLists();
                        tagContainer.setVisibility(View.VISIBLE);
                        popupRoot.findViewById(R.id.no_tag_text).setVisibility(View.GONE);
                        /*Log.d("TAG_CHECK", Arrays.toString(memdb.getTagsForFile(filePath)));*/
                        /*FlexboxLayout tagContainer = (FlexboxLayout) popupRoot.findViewById(R.id.tag_container);
                        addTagChip(tagContainer, tagName, file.getAbsolutePath());*/
                    } catch (Exception e){
                        /*new AlertDialog.Builder(ctx).setMessage(e.getMessage()).setTitle("Error").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterfaceErrorMsg, int i) {
                                dialogInterfaceErrorMsg.dismiss();
                            }
                        }).show();*/
                        Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    } finally {
                        tagNameInput.setText("");
                    }
                } else {
                    popupRoot.findViewById(R.id.popup_base).setVisibility(View.VISIBLE);
                    ((Button) view).setText("add");
                    popupRoot.findViewById(R.id.tag_name_input).findFocus();
                }
                tagAddFlag = !tagAddFlag;
            }
        });
    }

    public void openPopup(String filePath){
        loadData(filePath);
        open = true;
        popup.showAtLocation(view, Gravity.CENTER, 0, 0);
        popupRoot.findViewById(R.id.close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closePopup();
            }
        });
    }

    public void attachItem(MyAdapter.ViewHolder item){
        this.itemView_MyAdapter = item;
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                open = false;
                itemView_MyAdapter.populateData(file.getAbsoluteFile());
            }
        });
    }

    public void attachItem(FilterResultAdapter.ViewHolder item){
        this.itemView_FilterResultAdapter = item;
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                open = false;
                itemView_FilterResultAdapter.populateData(file.getAbsoluteFile());
            }
        });
    }

    public void closePopup(){
        popup.dismiss();
    }
}
