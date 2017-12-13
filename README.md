# Sync
Sync library for now only compatible with NextCloud



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
