rm -rf ./tmp/*
mkdir -p ./tmp
make > /dev/null

export LD_LIBRARY_PATH=$FUNKY_LIB_PATH/lib


for i in {1..100}
do

    ./tmp/test.run > ./tmp/res.txt
    find=`grep -n "<<<CLEAN_EXIT>>>" ./tmp/res.txt`

done

if [ "$find" = "" ]
then
	echo "group_test.funky did not terminate properly!" 
    cat ./tmp/res.txt
	exit 1
else
    exit 0
fi

