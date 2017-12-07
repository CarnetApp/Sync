package com.spisoft.sync;

/**
 * Created by phoenamandre on 05/12/17.
 */

public class Configuration {
    public static OnAccountCreatedListener sOnAccountCreatedListener;

    public interface OnAccountSelectedListener{
        public void onAccountSelected(int accountId, int accountType);
    }
    public interface OnAccountCreatedListener{
        public void onAccountCreated(int accountId, int accountType);
    }
    static public OnAccountSelectedListener sOnAccountSelectedListener = null;
}
