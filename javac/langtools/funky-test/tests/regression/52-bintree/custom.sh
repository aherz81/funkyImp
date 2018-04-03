rm -rf ./tmp/*
mkdir -p ./tmp
make > /dev/null
make compile2> /dev/null

export LD_LIBRARY_PATH=$FUNKY_LIB_PATH/lib


    ./tmp/sfib.run > ./tmp/sres.txt
    find=`grep "time: " ./tmp/sres.txt`


for i in {1..5}
do

    ./tmp/pfib.run > ./tmp/pres.txt
    find=`grep "time: " ./tmp/pres.txt`

done

if [ "$find" = "" ]
then
	echo "fib.funky did not terminate properly!" 
    cat ./tmp/pres.txt
	exit 1
else
    tp=`grep "main::sum" ./tmp/pres.txt | sed -e 's/.*main::sum.*: \(.*\)+.*/\1/g'`
    ts=`grep "main::sum" ./tmp/sres.txt | sed -e 's/.*main::sum.*: \(.*\)+.*/\1/g'`
    cmp=`echo $ts '>' $tp | bc`
    if [ "$cmp" = "1" ]
    then
        exit 0
    else
	echo "parallel version of fib.funky slower than sequential!" 
        exit 1        
    fi
fi

