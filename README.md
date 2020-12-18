# KGSplit
Analyzing and Splitting Knowledge Graphs with Statistical Guarantees.

## Installation prereqs
JDK 9+ and Gradle 6.6+. Optional: Git and Gephi.

## Installation (Windows)

git clone PROJECT_URL

### Copy source files

xcopy KGSplit\src KGSplit\export\Analyzer\src\ /E/H

xcopy KGSplit\src KGSplit\export\Comparer\src\ /E/H

xcopy KGSplit\src KGSplit\export\Exporter\src\ /E/H

xcopy KGSplit\src KGSplit\export\Loader\src\ /E/H

xcopy KGSplit\src KGSplit\export\Split\src\ /E/H

### Add Gradle wrapper and build

#### Analyzer

cd KGSplit\export\Analyzer\

gradle wrapper

gradlew build

gradlew install

cd ..\\..\\..

#### Comparer

cd KGSplit\export\Comparer\

gradle wrapper

gradlew build

gradlew install

cd ..\\..\\..

#### Exporter

cd KGSplit\export\Exporter\

gradle wrapper

gradlew build

gradlew install

cd ..\\..\\..

#### Loader

cd KGSplit\export\Loader\

gradle wrapper

gradlew build

gradlew install

cd ..\\..\\..

#### Split

cd KGSplit\export\Split\

gradle wrapper

gradlew build

gradlew install

cd ..\\..\\..
