package com.spisoft.sync.browsing;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.spisoft.sync.Log;
import com.spisoft.sync.R;
import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.wrappers.FileItem;

public class FilePickerActivity extends BrowserActivity {

    public static final String RESULT_PICKER_PATH = "picker_path";
}
