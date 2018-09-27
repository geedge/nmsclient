@echo off
@echo 创建密钥名称：%1 密钥库路径及名称：%2 证书路径及名称：%3
set aliasName=clientkey
set storePath=./client_ks
set local=localhost
set certName=./client_key.cer
if not "%1"=="" (
	set aliasName=%1
)
if not "%2"=="" (
	set storePath=%2
)
if not "%3"=="" (
	set certName=%3
)

@echo %aliasName% %storePath% %certName%
echo y|keytool -import -v -trustcacerts -alias %aliasName% -keystore %storePath% -file %certName% -storetype jceks -storepass client
