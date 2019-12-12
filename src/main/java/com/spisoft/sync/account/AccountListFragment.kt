package com.spisoft.sync.account


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import com.spisoft.sync.Configuration
import com.spisoft.sync.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class AccountListFragment : androidx.fragment.app.Fragment(), AdapterView.OnItemClickListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account_list, container, false)


    }

    private var mAddButton: View ?= null
    private var mListView: ListView ?= null

    private var mEmptyView: View?= null

    override public fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAddButton = view.findViewById<View>(R.id.addButton)
        mAddButton?.setOnClickListener(View.OnClickListener {
            startActivity(
                Intent(
                    context,
                    AccountTypeActivity::class.java
                )
            )
        })
        mListView = view.findViewById<ListView>(R.id.account_list)
        mEmptyView = view.findViewById<View>(R.id.empty_view)
        mListView?.setOnItemClickListener(this)
        refreshCursor()
    }

    private var mAdapter: AccountAdapter? = null

    private fun refreshCursor() {
        val cursor = DBAccountHelper.getInstance(context).cursor
        if (cursor == null || cursor.count == 0) {
            mEmptyView?.setVisibility(View.VISIBLE)
        } else {
            mEmptyView?.setVisibility(View.GONE)
            mAdapter = AccountAdapter(context, cursor, 0)
            mListView?.setAdapter(mAdapter)

        }


    }

    override fun onResume() {
        super.onResume()
        if (mAdapter != null)
            refreshCursor()
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        mAdapter!!.getCursor().moveToPosition(i)
        Configuration.sOnAccountSelectedListener.onAccountSelected(
            l.toInt(),
            mAdapter!!.getCursor()!!.getInt(mAdapter!!.getCursor().getColumnIndex(DBAccountHelper.KEY_ACCOUNT_TYPE))
        )
    }


}
