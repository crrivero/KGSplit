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

## Load graph (training, validation and test)

cd KGSplit\export\Loader

build\install\kgsplit_loader\bin\kgsplit_loader -db ..\..\FB13\ -i ..\..\..\OpenKE\benchmarks\FB13\train2id.txt -f NUMERIC -t sop -s \t -p 0

build\install\kgsplit_loader\bin\kgsplit_loader -db ..\..\FB13\ -i ..\..\..\OpenKE\benchmarks\FB13\valid2id.txt -f NUMERIC -t sop -s \t -p 1

build\install\kgsplit_loader\bin\kgsplit_loader -db ..\..\FB13\ -i ..\..\..\OpenKE\benchmarks\FB13\test2id.txt -f NUMERIC -t sop -s \t -p 2

