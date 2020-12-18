# KGSplit
Analyzing and Splitting Knowledge Graphs with Statistical Guarantees.

## Installation prereqs
JDK 9+ and Gradle 6.6+. Optional: Git and Gephi.

## Installation (Windows)

git clone PROJECT_URL

### Copy source files

xcopy KGSplit\src KGSplit\export\Analyzer\ /E/H

xcopy KGSplit\src KGSplit\export\Comparer\ /E/H

xcopy KGSplit\src KGSplit\export\Exporter\ /E/H

xcopy KGSplit\src KGSplit\export\Loader\ /E/H

xcopy KGSplit\src KGSplit\export\Split\ /E/H

### Add Gradle wrapper and build

cd KGSplit\export\Analyzer\

gradle wrapper

gradlew build

gradlew install

cd ..\..\..


cd KGSplit\export\Comparer\

gradle wrapper

gradlew build

gradlew install

cd ..\..\..


cd KGSplit\export\Exporter\

gradle wrapper

gradlew build

gradlew install

cd ..\..\..


cd KGSplit\export\Loader\

gradle wrapper

gradlew build

gradlew install

cd ..\..\..


cd KGSplit\export\Split\

gradle wrapper

gradlew build

gradlew install

cd ..\..\..
