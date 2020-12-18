# KGSplit
Analyzing and Splitting Knowledge Graphs with Statistical Guarantees.

## Installation prereqs
JDK 9+ and Gradle 6.6+. Optional: Git and Gephi.

## Installation (Windows)

git clone PROJECT_URL

### Copy source files

xcopy KGSplit\src KGSplit\export\MODULE\src\ /E/H

where MODULE=Analyzer, Comparer, Exporter, Loader, Split

### Add Gradle wrapper and build

Save the following as a .bat file.


cd KGSplit\export\MODULE\

call gradle wrapper

call gradlew build

call gradlew install

cd ..\\..\\..

where MODULE=Analyzer, Comparer, Exporter, Loader, Split
