package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MegaMonthPicLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.managerSections.CameraUploadFragmentLollipop;
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class MegaPhotoSyncGridTitleAdapterLollipop extends RecyclerView.Adapter<MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle> implements SectionTitleProvider {

    private class Media {
        public String filePath;
        public long timestamp;
    }

    DisplayMetrics outMetrics;

    public static int PADDING_GRID_LARGE = 6;
    public static int PADDING_GRID_SMALL = 3;

    public static final int TYPE_ITEM_TITLE = 3;
    public static final int TYPE_ITEM_IMAGE = 1;
    public static final int TYPE_ITEM_VIDEO = 2;
    public static final int TYPE_NO_TYPE = 0;

    private MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle holder = null;

    private Context context;
    private MegaApplication app;
    private MegaApiAndroid megaApi;

    private ArrayList<MegaMonthPicLollipop> monthPics;
    private ArrayList<MegaNode> nodes;

    private long photosyncHandle = -1;

    private RecyclerView listFragment;
    private ImageView emptyImageViewFragment;
    private LinearLayout emptyTextViewFragment;
    private ActionBar aB;

    private int numberOfCells;
    private int gridWidth;

    private boolean multipleSelect;

    private SparseBooleanArray checkedItems = new SparseBooleanArray();

    private int orderGetChildren = MegaApiJava.ORDER_MODIFICATION_DESC;

    private Object fragment;
    private int type = CAMERA_UPLOAD_ADAPTER;

    private ActionMode actionMode;

    private int count;
    private int countTitles;
    private ItemInformation dateNode;
    private String dateNodeText = null;

    private List<ItemInformation> itemInformationList;

    DatabaseHandler dbH;
    MegaPreferences prefs;
    String downloadLocationDefaultPath;
    String defaultPath;

    Handler handler;

    public static class ItemInformation{
        public int type = -1;
        public String name = null;
        public MegaNode n = null;
        public MegaMonthPicLollipop megaMonthPic = null;

        public ItemInformation(int type, String name, MegaMonthPicLollipop megaMonthPic){
            this.type = type;
            this.name = name;
            this.megaMonthPic = megaMonthPic;
        }

        public ItemInformation(int type, MegaNode n, MegaMonthPicLollipop megaMonthPic){
            this.type = type;
            this.n = n;
            this.megaMonthPic = megaMonthPic;
        }
    }

    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            logDebug("onActionItemClicked");
            List<MegaNode> documents = getSelectedDocuments();
            if (documents.isEmpty()) {
                return false;
            }

            switch (item.getItemId()) {
                case R.id.cab_menu_download:
                    clearSelections();
                    new NodeController(context)
                        .prepareForDownload(getDocumentHandles(documents), false);
                    break;
                case R.id.cab_menu_copy:
                    clearSelections();
                    new NodeController(context)
                        .chooseLocationToCopyNodes(getDocumentHandles(documents));
                    break;
                case R.id.cab_menu_move:
                    clearSelections();
                    new NodeController(context)
                        .chooseLocationToMoveNodes(getDocumentHandles(documents));
                    break;
                case R.id.cab_menu_share_out:
                    clearSelections();
                    MegaNodeUtil.shareNodes(context, documents);
                    break;
                case R.id.cab_menu_share_link:
                case R.id.cab_menu_edit_link:
                    logDebug("Public link option");
                    clearSelections();
                    if (documents.size() == 1
                        && documents.get(0).getHandle() != MegaApiJava.INVALID_HANDLE) {
                        ((ManagerActivityLollipop) context)
                            .showGetLinkActivity(documents.get(0).getHandle());
                    }
                    break;
                case R.id.cab_menu_remove_link:
                    logDebug("Remove public link option");
                    clearSelections();
                    if (documents.size() == 1) {
                        ((ManagerActivityLollipop) context)
                            .showConfirmationRemovePublicLink(documents.get(0));
                    }
                    break;
                case R.id.cab_menu_send_to_chat:
                    logDebug("Send files to chat");
                    clearSelections();
                    new NodeController(context).checkIfNodesAreMineAndSelectChatsToSendNodes(
                        (ArrayList<MegaNode>) documents);
                    break;
                case R.id.cab_menu_trash:
                    clearSelections();
                    ((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(
                        getDocumentHandles(documents));
                    break;
                case R.id.cab_menu_select_all:
                    selectAll();
                    break;
                case R.id.cab_menu_clear_selection:
                    clearSelections();
                    break;
            }
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            logDebug("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.cloud_storage_action, menu);
            ((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
            Util.changeStatusBarColor(context, ((ManagerActivityLollipop) context).getWindow(),
                R.color.accentColorDark);
            ((ManagerActivityLollipop) context).setDrawerLockMode(true);
            ((CameraUploadFragmentLollipop) fragment).checkScroll();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            logDebug("onDestroyActionMode");
            clearSelections();
            multipleSelect = false;
            actionMode = null;
            ((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final Window window = ((ManagerActivityLollipop) context).getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                handler.postDelayed(() -> window.setStatusBarColor(0), 350);
            }
            ((CameraUploadFragmentLollipop) fragment).checkScroll();
            ((ManagerActivityLollipop) context).setDrawerLockMode(false);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            logDebug("onPrepareActionMode");
            List<MegaNode> selected = getSelectedDocuments();
            if (selected.isEmpty()) {
                return false;
            }

            CloudStorageOptionControlUtil.Control control =
                new CloudStorageOptionControlUtil.Control();

            if (selected.size() == 1
                && megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode()
                == MegaError.API_OK) {
                if (selected.get(0).isExported()) {
                    control.manageLink().setVisible(true)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                    control.removeLink().setVisible(true);
                } else {
                    control.getLink().setVisible(true)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
            }

            menu.findItem(R.id.cab_menu_send_to_chat)
                .setIcon(mutateIconSecondary(context, R.drawable.ic_send_to_contact,
                    R.color.white));

            control.sendToChat().setVisible(true)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            control.shareOut().setVisible(true)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            control.trash().setVisible(MegaNodeUtil.canMoveToRubbish(selected));

            control.move().setVisible(true);
            control.copy().setVisible(true);
            if (selected.size() > 1) {
                control.move().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            control.selectAll().setVisible(selected.size() != count - countTitles);

            CloudStorageOptionControlUtil.applyControl(menu, control);

            return true;
        }
    }

    @Override
    public ViewHolderPhotoTitleSyncGridTitle onCreateViewHolder(ViewGroup parent, int viewType) {
        logDebug("onCreateViewHolder");

        dbH = DatabaseHandler.getDbHandler(context);
        prefs = dbH.getPreferences();
        downloadLocationDefaultPath = getDownloadLocation();

        handler = new Handler();

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = ((Activity) context).getResources().getDisplayMetrics().density;

        float scaleW = getScaleW(outMetrics, density);
        float scaleH = getScaleH(outMetrics, density);

        float dpHeight = outMetrics.heightPixels / density;
        float dpWidth  = outMetrics.widthPixels / density;

//		Toast.makeText(context, "W: " + dpWidth + "__H: " + dpHeight, Toast.LENGTH_SHORT).show();
//		Toast.makeText(context, "Wpx: " + outMetrics.widthPixels + "__H: " + outMetrics.heightPixels, Toast.LENGTH_SHORT).show();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.cell_photosync_grid_title, parent, false);

        int padding = 0;
        if(numberOfCells == CameraUploadFragmentLollipop.GRID_LARGE){
            logDebug("numOfCells is GRID_LARGE");

            padding = PADDING_GRID_LARGE;
        }
        else if (numberOfCells == CameraUploadFragmentLollipop.GRID_SMALL){
            logDebug("numOfCells is GRID_SMALL");
            padding = PADDING_GRID_SMALL;
        }
        else{
            logDebug("numOfCells is " + numberOfCells);
            padding = 2;
//				iV.setPadding(padding, padding, padding, padding);
        }
        holder = new MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle(v, gridWidth, padding, this);
        holder.setDocument(-1l);
//		//Margins
//		RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)holder.textView.getLayoutParams();
//		contentTextParams.setMargins(scaleWidthPx(63, outMetrics), scaleHeightPx(5, outMetrics), 0, scaleHeightPx(5, outMetrics));
//		holder.textView.setLayoutParams(contentTextParams);


        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderPhotoTitleSyncGridTitle holder, int position) {
//        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
//        DisplayMetrics outMetrics = new DisplayMetrics();
//        display.getMetrics(outMetrics);
//        float density = ((Activity) context).getResources().getDisplayMetrics().density;
//
//        float scaleW = getScaleW(outMetrics, density);
//        float scaleH = getScaleH(outMetrics, density);

        ItemInformation itemInformation = getInformationOfPosition(position);
        switch (itemInformation.type){
            case TYPE_ITEM_TITLE:{
                holder.setDataTitle(itemInformation.name);
                break;
            }
            case TYPE_ITEM_IMAGE:{
                holder.setMegaMonthPicLollipop(itemInformation.megaMonthPic);
                int positionNodes = (int) itemInformation.megaMonthPic.getPosition(itemInformation.n);
                holder.setDataImage(itemInformation.n, isChecked(positionNodes), multipleSelect, position, positionNodes);
                holder.setGridWidth(gridWidth, numberOfCells);

                break;
            }
            case TYPE_ITEM_VIDEO:{
                holder.setMegaMonthPicLollipop(itemInformation.megaMonthPic);
                int positionNodes = (int) itemInformation.megaMonthPic.getPosition(itemInformation.n);
                holder.setDataVideo(itemInformation.n, isChecked(positionNodes), multipleSelect, numberOfCells, position, positionNodes);
                holder.setGridWidth(gridWidth, numberOfCells);
                break;
            }
            case TYPE_NO_TYPE:
                logWarning("Error, NO TYPE");
                break;
        }


    }

    @Override
    public int getItemCount() {
        return count;
    }

    public class ViewHolderPhotoTitleSyncGridTitle extends RecyclerView.ViewHolder implements ThumbnailUtilsLollipop.ThumbnailInterface{
        private RelativeLayout layout_title;
        private TextView title;
        private RoundedImageView photo;
        private RelativeLayout layout_videoInfo;
        private TextView videoDuration;
        private ImageView videoIcon;
        private RelativeLayout gradient_effect;
        private ImageView click_icon;
        private RelativeLayout content_layout;
        private long document;
        private MegaMonthPicLollipop megaMonthPicLollipop;
        private int positionAdapter;
        private int positionNodes;
        private RecyclerView.Adapter adapter;
        private int type = MegaPhotoSyncGridTitleAdapterLollipop.TYPE_NO_TYPE;
        private int margins = 0;
        private int gridWidth = 0;

        public ViewHolderPhotoTitleSyncGridTitle(View itemView, int gridWidth, int margins, RecyclerView.Adapter adapter) {
            super(itemView);
            layout_title = (RelativeLayout) itemView.findViewById(R.id.cell_photosync_grid_title_layout);
            layout_title.setVisibility(View.GONE);
            title = (TextView) itemView.findViewById(R.id.cell_photosync_grid_title_title);
            photo = (RoundedImageView) itemView.findViewById(R.id.cell_photosync_grid_title_thumbnail);
            layout_videoInfo = (RelativeLayout) itemView.findViewById(R.id.cell_item_videoinfo_layout);
            layout_videoInfo.setVisibility(View.GONE);
            videoDuration = (TextView) itemView.findViewById(R.id.cell_photosync_grid_title_video_duration);
            gradient_effect = (RelativeLayout) itemView.findViewById(R.id.cell_photosync_title_gradient_effect);
            gradient_effect.setVisibility(View.GONE);
            click_icon = (ImageView) itemView.findViewById(R.id.cell_photosync_title_menu_long_click_select);
            RelativeLayout.LayoutParams paramsI = (RelativeLayout.LayoutParams) click_icon.getLayoutParams();
            if (((ManagerActivityLollipop)context).isSmallGridCameraUploads){
                paramsI.width = px2dp(16, outMetrics);
                paramsI.height = px2dp(16, outMetrics);
                paramsI.setMargins(px2dp(3, outMetrics), px2dp(3, outMetrics), 0, 0);
            }
            else {
                paramsI.width = px2dp(23, outMetrics);
                paramsI.height = px2dp(23, outMetrics);
                paramsI.setMargins(px2dp(7, outMetrics), px2dp(7, outMetrics), 0, 0);
            }
            click_icon.setLayoutParams(paramsI);
            videoIcon = (ImageView) itemView.findViewById(R.id.cell_photosync_grid_title_video_icon);
            content_layout = (RelativeLayout) itemView.findViewById(R.id.cell_item_grid_title_layout);

            this.margins = margins;
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) content_layout.getLayoutParams();
            marginParams.setMargins(margins, margins, margins, margins);
            content_layout.setLayoutParams(marginParams);

            this.gridWidth = gridWidth;
            ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) content_layout.getLayoutParams();
            params.height = gridWidth;
            params.width = gridWidth;
            content_layout.setLayoutParams(params);

            click_icon.setMaxHeight(gridWidth);
            click_icon.setMaxWidth(gridWidth);

            content_layout.setVisibility(View.GONE);

            this.adapter = adapter;

            positionAdapter = -1;
            positionNodes = -1;
            final MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle thisClass = this;
            photo.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if(type == MegaPhotoSyncGridTitleAdapterLollipop.TYPE_NO_TYPE || type == MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_TITLE){
                        return;
                    }
                    long handle = document;

                    MegaNode n = megaApi.getNodeByHandle(handle);
                    if (n != null){
                        onNodeClick(thisClass, thisClass.getPositionNodes());
                    }
                }
            } );

            photo.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    if(type == MegaPhotoSyncGridTitleAdapterLollipop.TYPE_NO_TYPE || type == MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_TITLE){
                        return true;
                    }
                    long handle = document;

                    MegaNode n = megaApi.getNodeByHandle(handle);
                    if (n != null){
                        int positionInNodes = thisClass.getPositionNodes();
                        onNodeLongClick(thisClass, positionInNodes);
                    }

                    return true;
                }
            });
        }

        public void setDataTitle(String txt){
            layout_title.setVisibility(View.VISIBLE);
            content_layout.setVisibility(View.GONE);
            title.setText(txt);

            type = MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_TITLE;
        }

        public void setDataImage(MegaNode n, boolean checked, boolean multipleSelect, int positionAdapter, int positionNodes){
            layout_title.setVisibility(View.GONE);
            content_layout.setVisibility(View.VISIBLE);
            layout_videoInfo.setVisibility(View.GONE);
            gradient_effect.setVisibility(View.GONE);

            this.positionAdapter = positionAdapter;
            this.positionNodes = positionNodes;


            type = MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_IMAGE;

            if (multipleSelect){
                if (checked){
                    click_icon.setImageResource(R.drawable.ic_select_folder);
                    photo.setBackground(ContextCompat.getDrawable(context,R.drawable.background_item_grid_selected));
                    photo.setPadding(px2dp(1, outMetrics), px2dp(1, outMetrics), px2dp(1, outMetrics), px2dp(1, outMetrics));
                }
                else{
                    click_icon.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
                    photo.setBackground(null);
                    photo.setPadding(0, 0, 0, 0);
                }
            }
            else{
                click_icon.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
                photo.setBackground(null);
                photo.setPadding(0, 0, 0, 0);
            }

            if(n == null){
                logError("n is null");
                return;
            }

            document = n.getHandle();

//            LoadImage task = new LoadImage(n, type, 0);
//            task.execute(holder);

            Bitmap thumb = null;
            photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
            if (multipleSelect && checked) {
                photo.setCornerRadius(px2dp(2, outMetrics));
            }
            else {
                photo.setCornerRadius(0);
            }
            postSetImageView();

            if (n.hasThumbnail()){
                thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(n);
                if (thumb != null){
                    if (multipleSelect && checked){
                        photo.setImageBitmap(ThumbnailUtilsLollipop.getRoundedBitmap(context,thumb,3));
                    }
                    else {
                        photo.setImageBitmap(thumb);
                    }
                    postSetImageView();
                }
                else{
                    thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(n, context);
                    if (thumb != null){
                        if (multipleSelect && checked){
                            photo.setImageBitmap(ThumbnailUtilsLollipop.getRoundedBitmap(context,thumb,3));
                        }
                        else {
                            photo.setImageBitmap(thumb);
                        }
                        postSetImageView();
                    }
                    else{
                        try{
                            thumb = ThumbnailUtilsLollipop.getThumbnailFromThumbnailInterface(n, context, this, megaApi, adapter);
                        }
                        catch(Exception e){} //Too many AsyncTasks

                        if (thumb != null){
                            if (multipleSelect && checked){
                                photo.setImageBitmap(ThumbnailUtilsLollipop.getRoundedBitmap(context,thumb,3));
                            }
                            else {
                                photo.setImageBitmap(thumb);
                            }
                            postSetImageView();
                        }
                        else{
                            photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
                            postSetImageView();
                        }
                    }
                }
            }
            else{
                logDebug(n.getHandle()+" NO ThUMB!!");
            }

        }

        public void setDataVideo(MegaNode n, boolean checked, boolean multipleSelect, int numberOfColumns, int positionAdapter, int positionNodes){
            layout_title.setVisibility(View.GONE);
            content_layout.setVisibility(View.VISIBLE);
            layout_videoInfo.setVisibility(View.VISIBLE);
            videoDuration.setVisibility(View.VISIBLE);
            gradient_effect.setVisibility(View.VISIBLE);

            this.positionAdapter = positionAdapter;
            this.positionNodes = positionNodes;

            type = MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_VIDEO;

            if (multipleSelect){
                if (checked){
                    click_icon.setImageResource(R.drawable.ic_select_folder);
                    photo.setBackground(ContextCompat.getDrawable(context,R.drawable.background_item_grid_selected));
                    photo.setPadding(px2dp(1, outMetrics), px2dp(1, outMetrics), px2dp(1, outMetrics), px2dp(1, outMetrics));
                    gradient_effect.setBackground(ContextCompat.getDrawable(context, R.drawable.gradient_cam_uploads_rounded));
                }
                else{
                    click_icon.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
                    photo.setBackground(null);
                    photo.setPadding(0, 0, 0, 0);
                    gradient_effect.setBackground(ContextCompat.getDrawable(context, R.drawable.gradient_cam_uploads));
                }
            }
            else{
                click_icon.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
                photo.setBackground(null);
                photo.setPadding(0, 0, 0, 0);
                gradient_effect.setBackground(ContextCompat.getDrawable(context, R.drawable.gradient_cam_uploads));
            }

            if(n == null){
                logError("n is null");
                return;
            }

            document = n.getHandle();

//            LoadImage task = new LoadImage(n, type, numberOfColumns);
//            task.execute(this);

            Bitmap thumb = null;
            photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
            if (multipleSelect && checked) {
                photo.setCornerRadius(px2dp(2, outMetrics));
            }
            else {
                photo.setCornerRadius(0);
            }
            postSetImageView();
            if (n.hasThumbnail()){
                thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(n);
                if (thumb != null){
                    if (multipleSelect && checked){
                        photo.setImageBitmap(ThumbnailUtilsLollipop.getRoundedBitmap(context,thumb,3));
                    }
                    else {
                        photo.setImageBitmap(thumb);
                    }
                    postSetImageView();
                }
                else{
                    thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(n, context);
                    if (thumb != null){
                        if (multipleSelect && checked){
                            photo.setImageBitmap(ThumbnailUtilsLollipop.getRoundedBitmap(context,thumb,3));
                        }
                        else {
                            photo.setImageBitmap(thumb);
                        }
                    }
                    else{
                        try{
                            thumb = ThumbnailUtilsLollipop.getThumbnailFromThumbnailInterface(n, context, this, megaApi, adapter);
                        }
                        catch(Exception e){} //Too many AsyncTasks

                        if (thumb != null){
                            if (multipleSelect && checked){
                                photo.setImageBitmap(ThumbnailUtilsLollipop.getRoundedBitmap(context,thumb,3));
                            }
                            else {
                                photo.setImageBitmap(thumb);
                            }
                        }
                        else{
                            photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
                        }
                        postSetImageView();
                    }
                }
            }
            else{
                logDebug(n.getHandle()+" NO ThUMB!!");
            }

            if(numberOfColumns == CameraUploadFragmentLollipop.GRID_LARGE){
                videoIcon.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                logDebug(n.getHandle() + " DURATION: " + n.getDuration());
                int duration = n.getDuration();
                if(duration>0){
                    videoDuration.setText(getVideoDuration(duration));
                }
                else{
                    videoDuration.setVisibility(View.GONE);
                }
            }
            else{
                videoIcon.setImageResource(R.drawable.ic_play_arrow_white_18dp);
                videoDuration.setVisibility(View.GONE);
            }
        }

        public long getDocument() {
            return document;
        }

        @Override
        public ImageView getImageView() {
            return photo;
        }

        @Override
        public int getPositionOnAdapter() {
            return positionAdapter;
        }

        @Override
        public void postSetImageView() {
        }

        @Override
        public void preSetImageView() {

        }

        @Override
        public void setBitmap(Bitmap bitmap) {
            photo.setImageBitmap(bitmap);
        }

        public void setDocument(long document) {
            this.document = document;
        }

        public void setMegaMonthPicLollipop(MegaMonthPicLollipop megaMonthPicLollipop) {
            this.megaMonthPicLollipop = megaMonthPicLollipop;
        }

        public int getPositionNodes() {
            return positionNodes;
        }

        public void setPositionNodes(int positionNodes) {
            this.positionNodes = positionNodes;
        }

        public int getGridWidth() {
            return gridWidth;
        }

        public void setGridWidth(int gridWidth, int numberOfCells) {
            if(this.gridWidth != gridWidth){
                this.gridWidth = gridWidth;
                ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) content_layout.getLayoutParams();
                params.height = gridWidth;
                params.width = gridWidth;
                content_layout.setLayoutParams(params);
                int padding = 0;
                if(numberOfCells == CameraUploadFragmentLollipop.GRID_LARGE){
                    logDebug("numOfCells is GRID_LARGE");

                    padding = PADDING_GRID_LARGE;
                }
                else if (numberOfCells == CameraUploadFragmentLollipop.GRID_SMALL){
                    logDebug("numOfCells is GRID_SMALL");
                    padding = PADDING_GRID_SMALL;
                }
                else{
                    logDebug("numOfCells is " + numberOfCells);
                    padding = 2;
                }
                setMargins(padding);
            }
        }

        public int getMargins() {
            return margins;
        }

        public void setMargins(int margins) {
            if(this.margins != margins){
                this.margins = margins;
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) content_layout.getLayoutParams();
                marginParams.setMargins(margins, margins, margins, margins);
                content_layout.setLayoutParams(marginParams);
            }
        }

//        private class LoadImage extends AsyncTask<ViewHolderPhotoTitleSyncGridTitle, Void, Bitmap> {
//
//            private MegaNode n;
//            private int type;
//            private int numberOfColumns;
//
//            public LoadImage(MegaNode n, int type, int numberOfColumns) {
//                this.n = n;
//                this.type = type;
//                this.numberOfColumns = numberOfColumns;
//            }
//
//            @Override
//            protected void onPreExecute(){
//                if(type == TYPE_ITEM_IMAGE){
//                    photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
//                }
//                else{
//                    photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
//                    if(numberOfColumns == CameraUploadFragmentLollipop.GRID_LARGE){
//                        videoIcon.setImageResource(R.drawable.ic_play_arrow_white_24dp);
//                        log(n.getName()+" DURATION: "+n.getDuration());
//                        int duration = n.getDuration();
//                        if(duration>0){
//                            int hours = duration / 3600;
//                            int minutes = (duration % 3600) / 60;
//                            int seconds = duration % 60;
//
//                            String timeString;
//                            if(hours>0){
//                                timeString = String.format("%d:%d:%02d", hours, minutes, seconds);
//                            }
//                            else{
//                                timeString = String.format("%d:%02d", minutes, seconds);
//                            }
//
//                            log("The duration is: "+hours+" "+minutes+" "+seconds);
//
//                            videoDuration.setText(timeString);
//                        }
//                        else{
//                            videoDuration.setVisibility(View.GONE);
//                        }
//                    }
//                    else{
//                        videoIcon.setImageResource(R.drawable.ic_play_arrow_white_18dp);
//                        videoDuration.setVisibility(View.GONE);
//                    }
//                }
//            }
//
//            @Override
//            protected Bitmap doInBackground(ViewHolderPhotoTitleSyncGridTitle... holders) {
//                if (n.hasThumbnail()){
//                    Bitmap thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(n);
//                    if (thumb != null){
//                        return thumb;
//                    }
//                    else{
//                        thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(n, context);
//                        if (thumb != null){
//                            return thumb;
//                        }
//                        else{
//                            try{
//                                thumb = ThumbnailUtilsLollipop.getThumbnailFromThumbnailInterface(n, context, holders[0], megaApi, adapter);
//                            }
//                            catch(Exception e){} //Too many AsyncTasks
//
//                            if (thumb != null){
//                                return thumb;
//                            }
//                        }
//                    }
//                }
//                else{
//                    log(n.getName()+" NO ThUMB!!");
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Bitmap result){
//                if(result != null){
//                    photo.setImageBitmap(result);
//                    postSetImageView();
//                }
//            }
//        };

    }

    public MegaPhotoSyncGridTitleAdapterLollipop(Context _context, ArrayList<MegaMonthPicLollipop> _monthPics, long _photosyncHandle, RecyclerView listView, ImageView emptyImageView, LinearLayout emptyTextView, ActionBar aB, ArrayList<MegaNode> _nodes, int numberOfCells, int gridWidth, Object fragment, int type, int count, int countTitles, List<ItemInformation> itemInformationList, String defaultPath) {
        this.context = _context;
        this.monthPics = _monthPics;
        this.photosyncHandle = _photosyncHandle;
        this.nodes = _nodes;

        this.listFragment = listView;
        this.emptyImageViewFragment = emptyImageView;
        this.emptyTextViewFragment = emptyTextView;
        this.aB = aB;
        this.fragment = fragment;
        this.type = type;
        this.numberOfCells = numberOfCells;
        this.gridWidth = gridWidth;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        this.app = ((MegaApplication) ((Activity) context).getApplication());

        this.count = count;
        this.countTitles = countTitles;

        this.itemInformationList = itemInformationList;

        this.defaultPath = defaultPath;
    }

    public void setNumberOfCells(int numberOfCells, int gridWidth){
        this.numberOfCells = numberOfCells;
        this.gridWidth = gridWidth;
        notifyDataSetChanged();
    }

    public void setNodes(ArrayList<MegaMonthPicLollipop> monthPics, ArrayList<MegaNode> nodes, int count, int countTitles, List<ItemInformation> itemInformationList){
        this.monthPics = monthPics;
        this.nodes = nodes;
        this.count = count;
        this.countTitles = countTitles;
        this.itemInformationList = itemInformationList;
        notifyDataSetChanged();
    }

    public void setContext(Context context) {
        logDebug("Context has been updated, the action mode needs to be cleaned");
        this.context = context;
        actionMode = null;
    }

    public Context getContext() {
        return this.context;
    }

    public void setPhotoSyncHandle(long photoSyncHandle){
        this.photosyncHandle = photoSyncHandle;
        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        if (monthPics.size() > position){
            return monthPics.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect){
        this.multipleSelect = multipleSelect;
    }

    public void setOrder(int orderGetChildren){
        this.orderGetChildren = orderGetChildren;
    }

    public long getPhotoSyncHandle(){
        return photosyncHandle;
    }

    @Override
    public String getSectionTitle(int position) {
        dateNode = getInformationOfPosition(position);
        if(dateNode != null){
            if(dateNode.megaMonthPic.monthYearString != null){
                if(dateNodeText == null){
                    dateNodeText = dateNode.megaMonthPic.monthYearString;
                }else if(!dateNodeText.equals(dateNode.megaMonthPic.monthYearString)){
                    dateNodeText = dateNode.megaMonthPic.monthYearString;
                }
            }
        }
        return dateNodeText;
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        logDebug("hideMultipleSelect");
        this.multipleSelect = false;
//        clearSelections();

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void selectAll(){
        this.multipleSelect = true;
        if (itemInformationList != null && nodes != null) {
            ItemInformation item;
            for (int i=0; i<itemInformationList.size(); i++) {
                item = itemInformationList.get(i);
                if (item.n != null) {
                    if (item.n.isFolder()) {
                        continue;
                    }
                    if (!MimeTypeThumbnail.typeForName(item.n.getName()).isImage() && (!MimeTypeThumbnail.typeForName(item.n.getName()).isVideo())){
                        continue;
                    }
                    checkedItems.append(nodes.indexOf(item.n), true);
                    startAnimation(null, i, false);
                }
            }
        }
//        if(nodes != null){
//            for(int i=0; i<nodes.size(); i++){
//                if (nodes.get(i).isFolder()){
//                    continue;
//                }
//
//                if (!MimeTypeThumbnail.typeForName(nodes.get(i).getName()).isImage() && (!MimeTypeThumbnail.typeForName(nodes.get(i).getName()).isVideo())){
//                    continue;
//                }
//                checkedItems.append(i, true);
//            }
//        }
        if (actionMode == null){
            actionMode = ((AppCompatActivity)context).startSupportActionMode(new MegaPhotoSyncGridTitleAdapterLollipop.ActionBarCallBack());
        }

        updateActionModeTitle();

        notifyDataSetChanged();
    }

    public void clearSelections() {
        logDebug("clearSelections");
        hideMultipleSelect();

        updateActionModeTitle();
        if (itemInformationList != null && nodes != null) {
            ItemInformation item;
            for (int i = 0; i < itemInformationList.size(); i++) {
                item = itemInformationList.get(i);
                if (item.n != null) {
                    int index = nodes.indexOf(item.n);
                    if (checkedItems.get(index, false) == true) {
                        logDebug("isChecked: " + index);
                        checkedItems.append(index, false);
                        startAnimation(null, i, true);
                    }
                }
            }
        }
//        for (int i = 0; i < checkedItems.size(); i++) {
//            if (checkedItems.valueAt(i) == true) {
//                int checkedPosition = checkedItems.keyAt(i);
//                checkedItems.append(checkedPosition, false);
//            }
//        }
        this.multipleSelect = false;
//        notifyDataSetChanged();
    }

    public boolean isChecked(int totalPosition){
        if (!multipleSelect){
            return false;
        }
        else{

            if (checkedItems.get(totalPosition, false) == false){
                return false;
            }
            else{
                return true;
            }
        }
    }

    public void onNodeClick(MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle holder, int positionInNodes){
        logDebug("Position in nodes: " + positionInNodes);

        if (!multipleSelect){
            MegaNode n = megaApi.getNodeByHandle(holder.getDocument());
            if (n != null){
                if (!n.isFolder()){
                    ImageView imageView = holder.getImageView();
                    int[] positionIV = new int[2];
                    imageView.getLocationOnScreen(positionIV);
                    int[] screenPosition = new int[4];
                    screenPosition[0] = positionIV[0];
                    screenPosition[1] = positionIV[1];
                    screenPosition[2] = imageView.getWidth();
                    screenPosition[3] = imageView.getHeight();

                    if (MimeTypeThumbnail.typeForName(n.getName()).isImage()){
                        Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
                        intent.putExtra("position", positionInNodes);
                        intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
                        intent.putExtra("orderGetChildren", orderGetChildren);
                        if(((ManagerActivityLollipop)context).isFirstNavigationLevel() == true){
                            intent.putExtra("adapterType", PHOTO_SYNC_ADAPTER);

                        }else{
                            intent.putExtra("adapterType", SEARCH_BY_ADAPTER);
                            long[] arrayHandles = new long[nodes.size()];
                            for(int i = 0; i < nodes.size(); i++) {
                                arrayHandles[i] = nodes.get(i).getHandle();
                            }
                            intent.putExtra("handlesNodesSearch",arrayHandles);
                        }

                        logDebug("Position in nodes: " + positionInNodes);
                        if (megaApi.getParentNode(nodes.get(positionInNodes)).getType() == MegaNode.TYPE_ROOT){
                            intent.putExtra("parentNodeHandle", -1L);
                        }
                        else{
                            intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(positionInNodes)).getHandle());
                        }
                        intent.putExtra("screenPosition", screenPosition);
                        context.startActivity(intent);
                        ((ManagerActivityLollipop) context).overridePendingTransition(0,0);
                        CameraUploadFragmentLollipop.imageDrag = imageView;
                    }
                    else if (MimeTypeThumbnail.typeForName(n.getName()).isVideoReproducible()){
                        MegaNode file = n;

                        String mimeType = MimeTypeThumbnail.typeForName(file.getName()).getType();
                        logDebug("File Handle: " + file.getHandle());

                        Intent mediaIntent;
                        boolean internalIntent;
                        if (MimeTypeThumbnail.typeForName(n.getName()).isVideoNotSupported()){
                            mediaIntent = new Intent(Intent.ACTION_VIEW);
                            internalIntent = false;
                        }
                        else {
                            internalIntent = true;
                            mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
                        }

                        mediaIntent.putExtra("position", positionInNodes);
                        if (megaApi.getParentNode(nodes.get(positionInNodes)).getType() == MegaNode.TYPE_ROOT){
                            mediaIntent.putExtra("parentNodeHandle", -1L);
                        }
                        else{
                            mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(positionInNodes)).getHandle());
                        }
                        mediaIntent.putExtra("orderGetChildren", orderGetChildren);
                        mediaIntent.putExtra("adapterType", PHOTO_SYNC_ADAPTER);

                        mediaIntent.putExtra("HANDLE", file.getHandle());
                        mediaIntent.putExtra("FILENAME", file.getName());
                        mediaIntent.putExtra("screenPosition", screenPosition);
                        if(((ManagerActivityLollipop)context).isFirstNavigationLevel() == true){
                            mediaIntent.putExtra("adapterType", PHOTO_SYNC_ADAPTER);

                        }else{
                            mediaIntent.putExtra("adapterType", SEARCH_BY_ADAPTER);
                            long[] arrayHandles = new long[nodes.size()];
                            for(int i = 0; i < nodes.size(); i++) {
                                arrayHandles[i] = nodes.get(i).getHandle();
                            }
                            mediaIntent.putExtra("handlesNodesSearch",arrayHandles);
                        }
                        String localPath = null;
                        try {
                            localPath = findVideoLocalPath(context, file);
                        } catch (Exception e) {
                            e.printStackTrace();
                            logWarning(e.getMessage());
                        }
                        if (localPath != null && checkFingerprint(megaApi,file,localPath)) {
                            File mediaFile = new File(localPath);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(file.getName()).getType());
                            }
                            else{
                                mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeThumbnail.typeForName(file.getName()).getType());
                            }
                            mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                        else {
                            if (megaApi.httpServerIsRunning() == 0) {
                                megaApi.httpServerStart();
                            }

                            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                            activityManager.getMemoryInfo(mi);

                            if(mi.totalMem>BUFFER_COMP){
                                logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                                megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                            }
                            else{
                                logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                                megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                            }

                            String url = megaApi.httpServerGetLocalLink(file);
                            mediaIntent.setDataAndType(Uri.parse(url), mimeType);
                        }
                        if (internalIntent) {
                            context.startActivity(mediaIntent);
                        }
                        else {
                            if (isIntentAvailable(context, mediaIntent)) {
                                context.startActivity(mediaIntent);
                            } else {
                                ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.intent_not_available), -1);
                                ArrayList<Long> handleList = new ArrayList<Long>();
                                handleList.add(n.getHandle());
                                NodeController nC = new NodeController(context);
                                nC.prepareForDownload(handleList, true);
                            }
                        }
                        ((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
                        CameraUploadFragmentLollipop.imageDrag = imageView;
                    }
                    else{
                        ArrayList<Long> handleList = new ArrayList<Long>();
                        handleList.add(n.getHandle());
                        NodeController nC = new NodeController(context);
                        nC.prepareForDownload(handleList, true);
                    }
                    notifyDataSetChanged();
                }
            }
        }
        else{
            boolean delete;
            if (checkedItems.get(positionInNodes, false) == false){
                checkedItems.append(positionInNodes, true);
                delete = false;
            }
            else{
                checkedItems.append(positionInNodes, false);
                delete = true;
            }

            startAnimation(holder, -1, delete);

            List<MegaNode> selectedNodes = getSelectedDocuments();
            if (selectedNodes.size() > 0){
                updateActionModeTitle();
//                notifyItemChanged(holder.getPositionOnAdapter());
            }
            else{
//                hideMultipleSelect();
                clearSelections();
            }
        }
    }

    void notifyItem (int type, final MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle holder, int pos) {
        if (type == 0) {
            notifyItemChanged(holder.getPositionOnAdapter());
        }
        else {
            notifyItemChanged(pos);
        }
    }

    void startAnimation (final MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle holder, final int pos, final boolean delete) {
        MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle view = (MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle)listFragment.findViewHolderForLayoutPosition(pos);
        int type;
        if ((holder != null && holder.click_icon != null)) {
            type = 0;
        }
        else {
            type = 1;
        }
        final int finalType = type;
        if ((holder != null && holder.click_icon != null) || view != null) {
            if ((holder != null && holder.click_icon != null)) {
                logDebug("Start animation: holderPosition: " + holder.getPositionOnAdapter());
            }
            else {
                logDebug("Start animation: position: " + pos);
            }
            Animation flipAnimation = AnimationUtils.loadAnimation(context,R.anim.multiselect_flip);
            notifyItem(finalType, holder, pos);
            flipAnimation.setDuration(200);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    logDebug("onAnimationStart");
                    if (!delete) {
                        notifyItem(finalType, holder, pos);
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    logDebug("onAnimationEnd");
                    if (delete) {
                        notifyItem(finalType, holder, pos);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            if (finalType == 0) {
                holder.click_icon.startAnimation(flipAnimation);
            }
            else {
                view.click_icon.startAnimation(flipAnimation);
            }
        }
        else {
            logWarning("View is null - not animation");
            notifyItem(finalType, holder, pos);
        }
    }

    public String getPath(String fileName, long fileSize, String destDir, MegaNode file) {
        logDebug("getPath");
        String path = null;
        if (destDir != null) {
            File dir = new File(destDir);
            File[] listFiles = dir.listFiles();

            if (listFiles != null) {
                for (int i = 0; i < listFiles.length; i++) {
                    if (listFiles[i].isDirectory()) {
                        path = getPath(fileName, fileSize, listFiles[i].getAbsolutePath(), file);
                    } else {
                        path = getLocalFile(context, fileName, fileSize);
                    }
                }
            }
        }

        return path;
    }

    public String getRealName (String photoSyncName) {
        String realName = null;
        String date = "";
        String time = "";
        String extension = "";
        boolean index = false;

        String[] s = photoSyncName.split(" ");
        if (s != null){
            if (s.length > 0){
                date = s[0];
                time = s[1];

                if (time != null) {
                    s = time.split("_");
                    if (s != null){
                        if (s.length > 0) {
                            time = s[0];
                            s = s[s.length-1].split("\\.");
                            extension = s[s.length-1];
                            index = true;
                        }
                    }
                    s = time.split("\\.");
                    if (s != null) {
                        if (s.length > 0) {
                            if (!index){
                                extension = s[s.length-1];
                            }
                            time = "";
                            for (int i= 0; i<s.length-1; i++){
                                time += s[i];
                            }
                        }
                    }
                }

                if (date != null) {
                    s = date.split("-");
                    if (s != null) {
                        if (s.length > 0) {
                            date = "";
                            for (int i=0; i<s.length; i++) {
                                date += s[i];
                            }
                        }
                    }
                }
            }
        }



        realName = date + "_" + time + "." + extension;

        return realName;
    }

    public void onNodeLongClick(MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle holder, int positionInNodes){
        logDebug("Position in nodes: " + positionInNodes);
        if (!multipleSelect){
            clearSelections();

            this.multipleSelect = true;
            checkedItems.append(positionInNodes, true);

            actionMode = ((AppCompatActivity)context).startSupportActionMode(new MegaPhotoSyncGridTitleAdapterLollipop.ActionBarCallBack());

            startAnimation(holder, -1, false);

            updateActionModeTitle();
        }
        else{
            onNodeClick(holder, positionInNodes);
        }
    }

    private void newActionMode() {
        logDebug("Force create new action mode");
        actionMode = ((AppCompatActivity) context).startSupportActionMode(new MegaPhotoSyncGridTitleAdapterLollipop.ActionBarCallBack());
    }

    private void updateActionModeTitle() {

        logDebug("updateActionModeTitle");
        if (actionMode == null){
            logError("actionMode null");
            return;
        }

        if (context == null){
            logError("Context null");
            return;
        }

        List<MegaNode> documents = getSelectedDocuments();
        int files = 0;
        int folders = 0;
        for (MegaNode document : documents) {
            if (document.isFile()) {
                files++;
            } else if (document.isFolder()) {
                folders++;
            }
        }

        Resources res = context.getResources();
        String title;
        int sum=files+folders;

        if (files == 0 && folders == 0) {
            title = Integer.toString(sum);
        } else if (files == 0) {
            title = Integer.toString(folders);
        } else if (folders == 0) {
            title = Integer.toString(files);
        } else {
            title = Integer.toString(sum);
        }
        actionMode.setTitle(title);
        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            e.printStackTrace();
            logError("Invalidate error", e);
        }


        // actionMode.
    }

    /*
     * Get list of all selected documents
     */
    public List<MegaNode> getSelectedDocuments() {
        logDebug("getSelectedDocuments");
        ArrayList<MegaNode> documents = new ArrayList<MegaNode>();
        for (int i = 0; i < checkedItems.size(); i++) {
            if (checkedItems.valueAt(i) == true) {
                MegaNode document = null;
                try {
                    if (nodes != null) {
                        document = nodes.get(checkedItems.keyAt(i));
                    }
                }
                catch (IndexOutOfBoundsException e) {}

                if (document != null){
                    documents.add(document);
                }
            }
        }

        return documents;
    }

    /**
     * Get handles for selected nodes.
     *
     * @return handles for selected nodes.
     */
    private ArrayList<Long> getDocumentHandles(List<MegaNode> documents) {
        ArrayList<Long> handles = new ArrayList<>();

        for (MegaNode node : documents){
            handles.add(node.getHandle());
        }

        return handles;
    }

    private int getTypeOfPosition(int position){
        for(MegaMonthPicLollipop temp : monthPics){
            logDebug("Size of temp: " + temp.nodeHandles);
            if(position == 0){
                return TYPE_ITEM_TITLE;
            }
            if(position > 0 && position-1 < temp.nodeHandles.size()){
                MegaNode n = megaApi.getNodeByHandle(temp.nodeHandles.get(position - 1));
                if(isVideoFile(n.getName())){
                    return TYPE_ITEM_VIDEO;
                }
                else{
                    return TYPE_ITEM_IMAGE;
                }
            }
            position -= (temp.nodeHandles.size() + 1);
        }
        return TYPE_NO_TYPE;
    }

    @Nullable
    private Object getItemOfPosition(int position){
        for(MegaMonthPicLollipop temp : monthPics){
            if(position == 0){
                logDebug("Title is " + temp.monthYearString);
                return temp.monthYearString;
            }
            if(position > 0 && position < temp.nodeHandles.size() + 1){
                return megaApi.getNodeByHandle(temp.nodeHandles.get(position - 1));
            }
            if(temp.nodeHandles.size() == 0)
                continue;
            position -= (temp.nodeHandles.size() + 1);
        }
        return null;
    }

    private ItemInformation getInformationOfPosition(int position){
        return itemInformationList.get(position);
    }

    private long getNodesPositionOfPosition(int position){
        for(MegaMonthPicLollipop temp : monthPics){
            if(position == 0){
                return -1;
            }
            if(position < temp.nodeHandles.size() + 1){
                return temp.getPosition(temp.nodeHandles.get(position - 1));
            }
            position -= (temp.nodeHandles.size() + 1);
        }
        return -1;
    }

    private MegaMonthPicLollipop getMegaMonthPicOfPosition(int position){
        for(MegaMonthPicLollipop temp : monthPics){
            if(position < temp.nodeHandles.size() + 1){
                return temp;
            }
            position -= (temp.nodeHandles.size() + 1);
        }
        return null;
    }

    public int getSpanSizeOfPosition(int position){
        int type = TYPE_NO_TYPE;
        try{
            type = itemInformationList.get(position).type;
        } catch (Exception e){
            e.printStackTrace();
        }
        switch (type){
            case TYPE_ITEM_TITLE:{
                return numberOfCells;
            }
            case TYPE_ITEM_IMAGE:
            case TYPE_ITEM_VIDEO:{
                return 1;
            }
            case TYPE_NO_TYPE:
                break;
        }
        return 0;
    }

    public void refreshActionModeTitle() {
        newActionMode();
        updateActionModeTitle();
        notifyDataSetChanged();
    }
}
