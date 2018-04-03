
#clean up
make clean

#build lib
make lib > /dev/null
#build project using lib
make import

export LD_LIBRARY_PATH=$FUNKY_LIB_PATH/lib


#run result
for i in {1..100}
do

    ./tmp/imported.run > ./tmp/res.txt
    find=`grep -n "time:" ./tmp/res.txt`

done

if [ "$find" = "" ]
then
	echo "imported.funky did not terminate properly!" 
    cat ./tmp/res.txt
	exit 1
else
    make clean
    exit 0
fi

