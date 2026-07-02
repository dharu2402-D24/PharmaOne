@echo off
echo ===================================
echo  PharmaOne - Building Application
echo ===================================

REM Create output directory
if not exist out mkdir out

REM Compile all Java files
echo Compiling Java files...
javac -cp "lib\*" -d out src\util\ThemeColors.java src\util\CustomIcons.java src\db\DatabaseConnection.java src\ui\HeaderPanel.java src\ui\StatusBar.java src\ui\Sidebar.java src\ui\DashboardPanel.java src\ui\InventoryPanel.java src\ui\OrdersPanel.java src\ui\SuppliersPanel.java src\ui\CustomersPanel.java src\ui\ReportsPanel.java src\ui\SettingsPanel.java src\ui\SQLConsolePanel.java src\ui\TriggerDemoPanel.java src\ui\DiscardedBatchesPanel.java src\ui\TransactionDemoPanel.java src\ui\MainFrame.java src\Main.java

if %ERRORLEVEL% neq 0 (
    echo Compilation FAILED!
    pause
    exit /b 1
)

echo Compilation successful!
echo.

REM --- Build standalone JAR (all dependencies bundled) ---
echo Building standalone JAR...

REM Create temp directory for JAR assembly
if exist jar_temp rmdir /s /q jar_temp
mkdir jar_temp

REM Copy compiled classes
xcopy /s /q out\* jar_temp\ >nul 2>&1

REM Extract dependency JARs into temp (merge into single JAR)
cd jar_temp
for %%f in (..\lib\*.jar) do (
    jar xf "%%f" >nul 2>&1
)
cd ..

REM Remove dependency META-INF (signatures etc.) to avoid conflicts
if exist jar_temp\META-INF rmdir /s /q jar_temp\META-INF

REM Create manifest
echo Main-Class: Main> jar_temp\MANIFEST.MF
echo.>> jar_temp\MANIFEST.MF

REM Build the fat JAR
jar cfm PharmaOne-Standalone.jar jar_temp\MANIFEST.MF -C jar_temp .

REM Clean up temp directory
rmdir /s /q jar_temp

if %ERRORLEVEL% neq 0 (
    echo JAR creation FAILED!
    pause
    exit /b 1
)

echo JAR created: PharmaOne-Standalone.jar
echo.
echo ===================================
echo  Build Complete!
echo ===================================
echo.
echo To run: java -jar PharmaOne-Standalone.jar
echo    -or- java -cp "out;lib\*" Main
echo.

REM Run the application
set /p RUN="Run the application now? (y/n): "
if /i "%RUN%"=="y" (
    echo.
    echo Starting PharmaOne Application...
    java -cp "out;lib\*" Main
)

pause
