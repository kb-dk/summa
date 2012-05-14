Summa-integration modulet er dele af control-modulet som er genoplivet. 
Mange unittest kan ikke køre(compilere) og er deleted. Grunden til de ikke kan køre
er at de benytter java filer fra control/src.(StorageService f.eks.). Unittest skal skriv om
så de kun benytter source filer fra eksisterende moduler.

Følgede unit-tests er deleted:
AutoDiscoverTest
DescriptorTest.java
FacetPerformanceTest.java
FagrefGeneratorTest.java
IndexTest.java
IngestTest.java
IterativeTest.java
MultipleSourcesTest.java
OAITest.java
ParentChildTest.java
RebuildFacetsTest.java
ScaleTest.java

Alle gamle unittests kan findes i folderen:old_unittests_to_be_fixed