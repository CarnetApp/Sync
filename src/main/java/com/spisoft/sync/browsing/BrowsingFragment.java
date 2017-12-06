package com.spisoft.sync.browsing;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.spisoft.sync.MainActivity;
import com.spisoft.sync.R;
import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.wrappers.AsyncLister;
import com.spisoft.sync.wrappers.FileItem;
import com.spisoft.sync.wrappers.Wrapper;
import com.spisoft.sync.wrappers.WrapperFactory;

import java.util.List;


public class BrowsingFragment extends Fragment implements FileListAdapter.Listener, AsyncLister.AsyncListerListener {
    private static final String ARG_ACCOUNT = "account";
    private static final String ARG_FILE_ITEM = "file_item";

    // TODO: Rename and change types of parameters
    private DBAccountHelper.Account mAccount;
    private FileItem mFileItem;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private FileListAdapter mFileAdapter;
    private Wrapper mWrapper;


    public BrowsingFragment() {
        // Required empty public constructor
    }


    public static BrowsingFragment newInstance(DBAccountHelper.Account account, FileItem item) {
        BrowsingFragment fragment = new BrowsingFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ACCOUNT, account);
        args.putSerializable(ARG_FILE_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAccount = (DBAccountHelper.Account) getArguments().getSerializable(ARG_ACCOUNT);
            mFileItem = (FileItem) getArguments().getSerializable(ARG_FILE_ITEM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_browsing, container, false);
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.recyclerview);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(false);
        mFileAdapter = new FileListAdapter(getContext());
        mFileAdapter.setListener(this);
        mRecyclerView.setAdapter(mFileAdapter);
        mWrapper = WrapperFactory.getWrapper(getActivity(),mAccount.accountType, mAccount.accountID);
        mWrapper.getAsyncLister(mFileItem==null?"/":mFileItem.getPath()).retrieveList(0, this);
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
            ((BrowserActivity)getActivity()).setFragment(BrowsingFragment.newInstance(mAccount,item));

    }

    @Override
    public void onMenuClick(FileItem item) {

    }

    @Override
    public void onListingResult(int requestCode, int resultCode, List<FileItem> list) {
        if(list!=null){
            mFileAdapter.setFileList(list);
        }
    }
}
