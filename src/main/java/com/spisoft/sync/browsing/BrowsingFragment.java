package com.spisoft.sync.browsing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.spisoft.sync.R;
import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.wrappers.AsyncLister;
import com.spisoft.sync.wrappers.FileItem;
import com.spisoft.sync.wrappers.Wrapper;
import com.spisoft.sync.wrappers.WrapperFactory;

import java.util.ArrayList;
import java.util.List;


public class BrowsingFragment extends Fragment implements FileListAdapter.Listener, AsyncLister.AsyncListerListener {
    private static final String ARG_ACCOUNT = "account";
    private static final String ARG_FILE_ITEM = "file_item";
    public static final String ARG_AS_FILE_PICKER = "as_file_picker";
    public static final String ARG_DISPLAY_ONLY_MIMETYPE = "display_only_mimetype";

    // TODO: Rename and change types of parameters
    private DBAccountHelper.Account mAccount;
    private FileItem mFileItem;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private FileListAdapter mFileAdapter;
    private Wrapper mWrapper;
    private String mDisplayMimetype;
    private boolean mAsFilePicker;
    private View mLoadingView;
    private View mEmptyView;


    public BrowsingFragment() {
        // Required empty public constructor
    }


    public static BrowsingFragment newInstance(DBAccountHelper.Account account, FileItem item, boolean asFilePicker, String displayOnlyMimetype) {
        BrowsingFragment fragment = new BrowsingFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ACCOUNT, account);
        args.putSerializable(ARG_FILE_ITEM, item);
        args.putSerializable(ARG_AS_FILE_PICKER, asFilePicker);
        args.putSerializable(ARG_DISPLAY_ONLY_MIMETYPE, displayOnlyMimetype);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAccount = (DBAccountHelper.Account) getArguments().getSerializable(ARG_ACCOUNT);
            mFileItem = (FileItem) getArguments().getSerializable(ARG_FILE_ITEM);
            mAsFilePicker = getArguments().getBoolean(ARG_AS_FILE_PICKER);
            mDisplayMimetype = getArguments().getString(ARG_DISPLAY_ONLY_MIMETYPE);
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0,R.string.pick_folder,0,R.string.pick_folder).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }


    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.string.pick_folder).setVisible(mAsFilePicker&&mDisplayMimetype!=null&&mDisplayMimetype.equals("DIR"));
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.string.pick_folder){
            Intent intent = new Intent();
            intent.putExtra(FilePickerActivity.RESULT_PICKER_PATH, mFileItem.getPath());
            getActivity().setResult(Activity.RESULT_OK,intent);
            getActivity().finish();
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_browsing, container, false);
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.recyclerview);
        mLoadingView = view.findViewById(R.id.loading_view);
        mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyView.setVisibility(View.GONE);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(false);
        mFileAdapter = new FileListAdapter(getContext());
        mFileAdapter.setListener(this);
        mRecyclerView.setAdapter(mFileAdapter);
        mWrapper = WrapperFactory.getWrapper(getActivity(),mAccount.accountType, mAccount.accountID);
        mLoadingView.setVisibility(View.VISIBLE);
        mLoadingView.setAlpha(1);
        mWrapper.getAsyncLister(mFileItem==null?"":mFileItem.getPath()).retrieveList(0, this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onFileClick(FileItem item) {
        if(item.isDirectory())
            ((BrowserActivity)getActivity()).setFragment(BrowsingFragment.newInstance(mAccount,item,mAsFilePicker,mDisplayMimetype));

    }

    @Override
    public void onMenuClick(FileItem item) {

    }

    @Override
    public void onListingResult(int requestCode, int resultCode, List<FileItem> list) {
        //filter
        if(list!=null){
            List<FileItem> items = new ArrayList<>(list);
            if(mDisplayMimetype!=null){
                for(FileItem item : list){
                    if(!item.isDirectory() && !item.getMimetype().equals(mDisplayMimetype))
                        items.remove(item);
                }
            }
            mFileAdapter.setFileList(items);
            if(items.size() == 0)
                mEmptyView.setVisibility(View.VISIBLE);
        } else mEmptyView.setVisibility(View.VISIBLE);

        if(Build.VERSION.SDK_INT >= 16)
            mLoadingView.animate().setDuration(500).alpha(0).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mLoadingView.setVisibility(View.GONE);
                }
            }).start();
        else mLoadingView.setVisibility(View.GONE);

    }
}
