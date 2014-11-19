
set path=G:\soft\DevTools\;%path%
cd /d D:\project\MyshLib\cluster\src\main\resources\mysh\cluster\rpc\thrift

thrift --gen java:private-members,java5 --out ../../../../../java TClusterService.thrift

pause
