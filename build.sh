#!/usr/bin/env bash
set -e

echo "==================================="
echo " PharmaOne - Building Application"
echo "==================================="

# Create output directory
mkdir -p out

# Compile all Java files
echo "Compiling Java files..."
javac -cp "lib/*" -d out \
    src/util/ThemeColors.java \
    src/util/CustomIcons.java \
    src/db/DatabaseConnection.java \
    src/ui/HeaderPanel.java \
    src/ui/StatusBar.java \
    src/ui/Sidebar.java \
    src/ui/DashboardPanel.java \
    src/ui/InventoryPanel.java \
    src/ui/OrdersPanel.java \
    src/ui/SuppliersPanel.java \
    src/ui/CustomersPanel.java \
    src/ui/ReportsPanel.java \
    src/ui/SettingsPanel.java \
    src/ui/SQLConsolePanel.java \
    src/ui/TriggerDemoPanel.java \
    src/ui/DiscardedBatchesPanel.java \
    src/ui/TransactionDemoPanel.java \
    src/ui/MainFrame.java \
    src/Main.java

echo "Compilation successful!"
echo ""

# --- Build standalone JAR (all dependencies bundled) ---
echo "Building standalone JAR..."

# Create temp directory for JAR assembly
rm -rf jar_temp
mkdir jar_temp

# Copy compiled classes
cp -r out/* jar_temp/

# Extract dependency JARs into temp (merge into single JAR)
for jarfile in lib/*.jar; do
    (cd jar_temp && jar xf "../$jarfile" 2>/dev/null || true)
done

# Remove dependency META-INF (signatures etc.) to avoid conflicts
rm -rf jar_temp/META-INF

# Create manifest
printf "Main-Class: Main\n\n" > jar_temp/MANIFEST.MF

# Build the fat JAR
jar cfm PharmaOne-Standalone.jar jar_temp/MANIFEST.MF -C jar_temp .

# Clean up temp directory
rm -rf jar_temp

echo "JAR created: PharmaOne-Standalone.jar"
echo ""
echo "==================================="
echo " Build Complete!"
echo "==================================="
echo ""
echo "To run: java -jar PharmaOne-Standalone.jar"
echo "   -or- java -cp 'out:lib/*' Main"
