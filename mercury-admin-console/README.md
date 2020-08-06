

## Create admin user in keystore

From extend install dir (where deploy and config dirs are) run following:


```
keytool  -keystore deploy/mercury-data/config/accounts.keystore -storepass password1234 -genkeypair -alias admin -keyalg RSA -dname "CN=admin"
```
