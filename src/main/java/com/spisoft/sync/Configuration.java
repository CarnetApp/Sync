package com.spisoft.sync;

/**
 * Created by phoenamandre on 05/12/17.
 */

public class Configuration {
    public interface OnAccountSelectedListener{
        public void onAccountSelected(long accountId, int accountType);
    }
    static public OnAccountSelectedListener sOnAccountSelectedListener = null;
}
