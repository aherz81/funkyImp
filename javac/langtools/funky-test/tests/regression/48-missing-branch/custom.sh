rm -rf ./tmp/*
make  > /dev/null
export LD_LIBRARY_PATH=$FUNKY_LIB_PATH/lib


for i in {1..100}
do

    ./tmp/test.run > ./tmp/res.txt
    find=`grep -n "<<<CLEAN_EXIT>>>" ./tmp/res.txt`
    
done

if [ "$find" = "" ]
then
	echo "branch.funky did not terminate properly!" 
	exit 1
else
    exit 0
fi

