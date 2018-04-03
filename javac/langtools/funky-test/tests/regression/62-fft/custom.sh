rm -rf ./tmp/*
mkdir -p ./tmp
make release > /dev/null

export LD_LIBRARY_PATH=$FUNKY_LIB_PATH/lib

./tmp/test.run > ./tmp/res.txt


if [ "$find" = "" ]
then
    exit 0
else
	echo "test did not terminate properly!" 
	exit 1
fi

