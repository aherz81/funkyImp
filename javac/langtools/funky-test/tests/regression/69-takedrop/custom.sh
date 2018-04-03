rm -rf ./tmp/*
echo "$LD_LIBRARY_PATH"
make > /dev/null
export LD_LIBRARY_PATH=$FUNKY_LIB_PATH/lib

for i in {1..10}
do
    ./tmp/test.run > ./tmp/res.txt
    find=`grep -n "19; 0" ./tmp/res.txt`

done

if [ "$find" = "" ]
then
	echo "takedrop did not terminate properly!" 
	exit 1
else
    exit 0
fi

