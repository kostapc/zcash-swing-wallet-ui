@echo off

SET kotodir=%appdata%\Koto
SET kotoconf=%kotodir%\koto.conf
SET sproutdir=%appdata%\ZcashParams
SET sprout-proving_url=http://d.koto.cash/sprout-proving.key?a=.js
SET sprout-verifying_url=http://d.koto.cash/sprout-verifying.key?a=.js
SET sprout-proving=%sproutdir%\sprout-proving.key
SET sprout-verifying=%sproutdir%\sprout-verifying.key
SET sprout-proving-hash=8bc20a7f013b2b58970cddd2e7ea028975c88ae7ceb9259a5344a16bc2c0eef7
SET sprout-verifying-hash=4bd498dae0aacfd8e98dc306338d017d9c08dd0918ead18172bd0aec2fc5df82


rem ###################################################
rem kotodirがあるかチェック
if exist %kotodir% goto EXIST_KOTODIR
mkdir %kotodir%
:EXIST_KOTODIR


rem ###################################################
rem koto.confがあるかチェック
if exist %kotoconf% goto EXIST_KOTOCOIN
echo koto.confを作成します。
echo rpcuser=%USERNAME%_%RANDOM% > %kotoconf%
echo rpcpassword=%USERNAME%_%RANDOM%_%RANDOM% >> %kotoconf%
:EXIST_KOTOCOIN


rem ###################################################
rem sproutdirがあるかチェック
if exist %sproutdir% goto EXIST_SPROUTDIR
mkdir %sproutdir%
:EXIST_SPROUTDIR

rem ###################################################
rem sprout-provingがあるかチェック
if exist %sprout-proving% goto EXIST_PROVING

:DOWNLOAD_PROVING
rem 無かったらダウンロード
echo sprout-proving.keyのダウンロードを開始します。
bitsadmin.exe /TRANSFER DOWNLOAD_PROVING %sprout-proving_url% %sprout-proving%

:EXIST_PROVING
rem 有ったらハッシュをチェック
echo sprout-provingのハッシュをチェック中
@FOR /F "delims=/" %%i IN ('CERTUTIL -hashfile %sprout-proving% SHA256 ^| FIND /V ":"') DO @((SET hash_sha256=%%i))
set proving_sha256=%hash_sha256: =%

if not %proving_sha256%==%sprout-proving-hash% (
echo ハッシュが一致しませんでした。
del /Q %sprout-proving%
goto DOWNLOAD_PROVING
)
:END_PROVING


rem ###################################################
rem sprout-verifyingがあるかチェック
if exist %sprout-verifying% goto EXIST_VERIFYING

:DOWNLOAD_VERIFYING
rem 無かったらダウンロード
echo sprout-verifying.keyのダウンロードを開始します。
bitsadmin.exe /TRANSFER DOWNLOAD_VERIFYING %sprout-verifying_url% %sprout-verifying%

:EXIST_VERIFYING
rem 有ったらハッシュをチェック
echo sprout-verifyingのハッシュをチェック中
@FOR /F "delims=/" %%i IN ('CERTUTIL -hashfile %sprout-verifying% SHA256 ^| FIND /V ":"') DO @((SET hash_sha256=%%i))
set verifying_sha256=%hash_sha256: =%

if not %verifying_sha256%==%sprout-verifying-hash% (
echo ハッシュが一致しませんでした。
del /Q %sprout-verifying%
goto DOWNLOAD_VERIFYING
)
:END_VERIFYING


rem ###################################################
echo 初期設定が終了しました。
echo マイニングなどを行う場合は、%kotoconf%に書かれているユーザ名、パスワードを確認してください。
