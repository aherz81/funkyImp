#JAVA_HOME=`/usr/libexec/java_home -v 1.8`
BASE_DIR=`pwd`/generated
RESULT_DIR=`pwd`/result
BASE_LEN=${#BASE_DIR}

rm -rf statistics
mkdir -p statistics
for subdir in $BASE_DIR/* ; do
	cd $subdir/
	TEST_NAME=`echo ${subdir:BASE_LEN+1} | sed 's/test_//' | sed 's/.f//'`	
	java -jar ../../TestResultQualityCheck.jar ../../automated.properties $subdir/resultTable.log > $subdir/averageResult.tmp
	echo $TEST_NAME > $subdir/testname.tmp
	paste $subdir/testname.tmp $subdir/averageResult.tmp >> ../../statistics/average.csv
done
