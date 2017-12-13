# Sync
Sync library for now only compatible with NextCloud
This is a work in progress


Supports :
- add multiple NextCloud accounts
- sync local folder to specified remote folder





Usage:

with provided account manager
Start account.AccountListActivity to display a list of currently synced folders
(+) floating button to add an account, select NextCloud (the only supported cloud for now) this will open nextcloud authorize activity



manually
```java
DBAccountHelper.Account account = DBAccountHelper.getInstance(AccountTypeActivity.this)
      .addOrReplaceAccount(new DBAccountHelper.Account(-1, NextCloudWrapper.ACCOUNT_TYPE, friendly_name));
WrapperFactory.getWrapper(context, NextCloudWrapper.ACCOUNT_TYPE, account.accountID).startAuthorizeActivityForResult(context, NEW_ACCOUNT_REQUEST);
```


add folder to sync
```java
WrapperFactory.getWrapper(context, NextCloudWrapper.ACCOUNT_TYPE, account.accountID).addFolderSync(local_path, remote_path)
```


add on account selected listener (when user selects an account on AccountListActivity)
```java
  Configuration.sOnAccountSelectedListener = new Configuration.OnAccountSelectedListener() {
      @Override
      public void onAccountSelected(int accountId, int accountType) {

      }
  };
```
On account created (will be called after authorization)
```java
        Configuration.sOnAccountCreatedListener = new Configuration.OnAccountCreatedListener() {
            @Override
            public void onAccountCreated(int accountId, int accountType) {
                startAccountConfigActivity(accountId, accountType);
            }
        };
  ```
 add path observer (will be called when a file or folder is locally modified)
```java
Configuration.addPathObserver(local_path, this);
```
        
File picker / browser
```java
  Intent intent = new Intent(getActivity(), FilePickerActivity.class);
  intent.putExtra(FilePickerActivity.EXTRA_ACCOUNT_ID, mAccountId);
  intent.putExtra(FilePickerActivity.EXTRA_START_PATH, mCurrentlySetPath);
  intent.putExtra(FilePickerActivity.EXTRA_AS_FILE_PICKER, true);
  intent.putExtra(FilePickerActivity.EXTRA_DISPLAY_ONLY_MIMETYPE, "DIR");
  startActivityForResult(intent,REQUEST_FILE_PICK);
 ```
