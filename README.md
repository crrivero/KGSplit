# KGWrangler
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

## Analysis

cd KGSplit\export\Analyzer

build\install\kgsplit_analyzer\bin\kgsplit_analyzer -db ..\\..\FB13\ -c Indeg_KS_Each_.05 Outdeg_KS_Each_.05 NonZero_All > ..\\..\FB13_Analysis_KS_.05.txt

build\install\kgsplit_analyzer\bin\kgsplit_analyzer -db ..\\..\FB13\ -c Indeg_C_Each_.05 Outdeg_C_Each_.05 NonZero_All > ..\\..\FB13_Analysis_C_.05.txt

It will run two analyses over the training split with respect to the original graph. The first one uses Kolmogorov-Smirnov with significance level .05 for indegrees and outdegrees of each predicate. Furthermore, we analyze whether there are isolated nodes in the training split regardless of predicate, i.e., nodes whose all incoming and outgoing edges are not in training.

## Compute new splits

cd KGSplit\export\Split

build\install\kgsplit_split\bin\kgsplit_split -db ..\\..\FB13\ -c Indeg_KS_Each_.05 Outdeg_KS_Each_.05 NonZero_All Percentage_80_Each -n split_KS_zerofive > ..\\..\FB13_Split_KS_.05.txt

build\install\kgsplit_split\bin\kgsplit_split -db ..\\..\FB13\ -c Indeg_C_Each_.05 Outdeg_C_Each_.05 NonZero_All Percentage_80_Each -n split_C_zerofive > ..\\..\FB13_Split_C_.05.txt

These splits will be stored in properties: split_KS_zerofive and split_C_zerofive, respectively. We enforce that the percentage of total triples of each predicate in the training split should remain above 80%.

## Compare splits

cd KGSplit\export\Comparer

build\install\kgsplit_comparer\bin\kgsplit_comparer -db ..\\..\FB13\ -ns split -nt split_C_zerofive -ps !0 -pt !0 >> ..\\..\FB13_Comparison.txt

build\install\kgsplit_comparer\bin\kgsplit_comparer -db ..\\..\FB13\ -ns split -nt split_KS_zerofive -ps !0 -pt !0 >> ..\\..\FB13_Comparison.txt

build\install\kgsplit_comparer\bin\kgsplit_comparer -db ..\\..\FB13\ -ns split_KS_zerofive -nt split_C_zerofive -ps !0 -pt !0 >> ..\\..\FB13_Comparison.txt

Three different splits (split, split_KS_zerofive, split_C_zerofive) will be compared. The '!0' means 'exclude training', i.e., the union of validation and test splits will be compared.

## Exporter

cd KGSplit\export\Exporter

build\install\kgsplit_exporter\bin\kgsplit_exporter -db ..\\..\FB13\ -o ..\\..\FB13_12.graphml -f GraphML -r 12 -n split split_KS_zerofive split_C_zerofive

It will create the 'FB13_12.graphml' file containing all triples in FB13 for predicate 12 with the split information previously computed.
