JAVA_HOME=`/usr/libexec/java_home -v 1.8`
CUR_DIR=`pwd`
SOURCE_DIR=$CUR_DIR/tmp/
TEMPLATE_DIR=$CUR_DIR/template/
TARGET_DIR=$CUR_DIR/generated/


NUM_EXAMPLES=1
if [ $# = 1 ]; then
	NUM_EXAMPLES=$1	
fi

echo ------------------------------------------------------
echo Generated Source Directory:	$SOURCE_DIR
echo Template Directory:		$TEMPLATE_DIR
echo Target Directory:			$TARGET_DIR
echo Number of Generated files:		$NUM_EXAMPLES
cat automated.properties
echo ------------------------------------------------------
mkdir -p tmp
java -jar FunkySampleCodeGenerator.jar automated.properties
rm -rf $TARGET_DIR
mkdir -p $TARGET_DIR
SRC_PATH_LEN=${#SOURCE_DIR}
for f in $SOURCE_DIR*; do
	NAME_ONLY=${f:$SRC_PATH_LEN}
	DIR_NAME=test_$NAME_ONLY
	mkdir $TARGET_DIR$DIR_NAME
	cp $TEMPLATE_DIR/* $TARGET_DIR$DIR_NAME/
	cp $f $TARGET_DIR$DIR_NAME/cur.f
done
