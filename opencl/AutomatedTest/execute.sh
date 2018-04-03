JAVA_HOME=`/usr/libexec/java_home -v 1.8`
BASE_DIR=`pwd`/generated
RESULT_DIR=`pwd`/result
mkdir -p $RESULT_DIR
BASE_LEN=${#BASE_DIR}

for subdir in $BASE_DIR/* ; do
	cd $subdir/
	TEST_NAME=`echo ${subdir:BASE_LEN+1} | sed 's/test_//' | sed 's/.f/.csv/'`	
	echo "Building $subdir"
	make | grep transpose | sed 's|Cost analysis[(]transpose[_0-9]*[)]: ||' > $subdir/build.log
	echo "executing $subdir"	
	tmp/test.run > $subdir/intermediate.log
	cat intermediate.log | grep transpose | grep GPURun | sed 's/#TIME(transpose_[0-9]*_[0-9]*::GPURun)@[0-9]*: //' | sed 's| +/- |	|' | sed 's/ [^0-9]\{2,4\} [(][0-9]* samples[)]//' > $subdir/output.log
	cat intermediate.log | grep transpose | grep CopyTo | sed 's/#TIME(transpose_[0-9]*_[0-9]*::CopyTo)@[0-9]*: //' | sed 's| +/- |	|' | sed 's/ [^0-9]\{2,4\} [(][0-9]* samples[)]//' > $subdir/toDevice.log
	cat intermediate.log | grep transpose | grep copyBack | sed 's/#TIME(transpose_[0-9]*_[0-9]*::copyBack)@[0-9]*: //' | sed 's| +/- |	|' | sed 's/ [^0-9]\{2,4\} [(][0-9]* samples[)]//' > $subdir/fromDevice.log
	echo "Elements	Transfer Time	Predicted Execution	Actual Execution	Standard Deviation"
	paste  $subdir/sizes.log  $subdir/build.log  $subdir/output.log > $subdir/resultTable.log
	paste  $subdir/sizes.log  $subdir/toDevice.log $subdir/fromDevice.log > $subdir/memoryTable.log
	cp $subdir/resultTable.log $RESULT_DIR/$TEST_NAME
	#rm $subdir/build.log $subdir/intermediate.log $subdir/output.log $subdir/toDevice.log $subdir/fromDevice.log
done
