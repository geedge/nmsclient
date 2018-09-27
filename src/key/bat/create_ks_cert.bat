@echo off
set aliasName=clientkey
set storePath=./client_ks
set local=192.168.10.100
set keyPass=123123
set certName=./client_key.cer
if not "%1"=="" (
	set aliasName=%1
)
if not "%2"=="" (
	set storePath=%2
)
if not "%3"=="" (
	set local=%3
)
if not "%4"=="" (
	set keyPass=%4
)
if not "%5"=="" (
	set certName=%5
)
@echo %aliasName% %storePath% %local% %keyPass% %certName%
keytool -genkey -v -alias %aliasName% -keyalg RSA -storetype jceks -keystore %storePath% -validity 90 -dname "CN=%local%,OU=cn,O=cn,L=cn,ST=cn,C=cn" -storepass client -keypass %keyPass%
keytool -export -alias %aliasName% -storetype jceks -keystore %storePath% -file %certName% -storepass client 
