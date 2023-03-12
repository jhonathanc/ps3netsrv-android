@echo off

echo|set /p="git.commit.id=" > src\main\assets\git.properties
git log -1 --pretty=format:'%%H' >> src\main\assets\git.properties
