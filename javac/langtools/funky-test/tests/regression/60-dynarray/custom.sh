rm -rf ./tmp/*
make release > /dev/null

export LD_LIBRARY_PATH=$FUNKY_LIB_PATH/lib

./tmp/test.run > ./tmp/res.txt
find=`grep -n "retval: 5" ./tmp/res.txt`

if [ "$find" = "" ]
then
	echo "test did not terminate properly!" 
	exit 1
else
    exit 0
fi

